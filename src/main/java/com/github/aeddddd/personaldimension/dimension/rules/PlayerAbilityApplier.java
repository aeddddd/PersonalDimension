package com.github.aeddddd.personaldimension.dimension.rules;

import com.github.aeddddd.personaldimension.dimension.PersonalDimensionRules;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Abilities;

import javax.annotation.Nullable;

/**
 * 统一应用/重置个人维度的玩家能力(飞行,移动速度,飞行惯性).
 *
 * <p>1.20 官方映射下可直接访问 {@link Abilities},不再需要 1.12 的 MCP/SRG 双名反射.</p>
 *
 * <p>为不破坏其他模组授予的能力,本模组在首次修改玩家能力前会把原值备份到
 * {@code player.getPersistentData()} 并打标;重置时仅在有标记的情况下还原到备份值,
 * 无标记则完全不动玩家能力.该备份随玩家存档落盘并在重生时被原版复制,
 * 因此跨重进/重生仍能精确还原.</p>
 *
 * <p><b>移动速度必须落到 {@link Attributes#MOVEMENT_SPEED} 的基础值上</b>:
 * 1.20 的 {@link Abilities#setWalkingSpeed} 只参与视野缩放,实际步行速度取自该属性;
 * 而原版读取玩家存档时会用 {@code abilities.walkSpeed} 覆盖属性基础值
 * ({@code Player#readAdditionalSaveData}).只还原 {@link Abilities} 而不管属性,
 * 会让个人维度的速度数值随玩家存档泄漏到其他维度(重进存档后尤为明显).
 * 备份中的 {@code movementSpeed} 记录进入维度前的属性基础值,离开时一并还原.</p>
 */
public final class PlayerAbilityApplier {

    private PlayerAbilityApplier() {
    }

    /**
     * 能力备份在玩家持久化 NBT 中的键.存在该键即表示当前能力由本模组授予.
     */
    private static final String BACKUP_KEY = "personal_dimension_pd_ability_backup";

    /**
     * 备份中记录的移动属性基础值键(进入维度前的原值).
     */
    private static final String MOVEMENT_SPEED_KEY = "movementSpeed";

    /**
     * 浮动比较容差.
     */
    private static final float EPSILON = 1e-4f;

    /**
     * 属性基础值比较容差.
     */
    private static final double BASE_EPSILON = 1e-6;

    /**
     * 按玩家当前所处维度收敛能力:处于个人维度内时施加规则,不在个人维度内时还原备份.
     *
     * <p>{@code rules} 为 {@code null} 表示玩家不在任何个人维度内.两个分支都只在
     * "当前状态与目标状态不一致"时写入,因此可以每 tick 调用:传送,维度删除,
     * 服务器停止等路径即便漏发事件,也会在一 tick 内自行收敛,不会把维度内的
     * 能力(飞行/移动速度)留在其他维度.</p>
     *
     * @param player 目标玩家
     * @param rules  玩家当前所处个人维度的规则;不在个人维度内时为 null
     */
    public static void reconcile(ServerPlayer player, @Nullable PersonalDimensionRules rules) {
        if (rules != null) {
            applyCapabilities(player, rules);
            tickNoFlightInertia(player, rules);
        } else {
            resetAbilities(player);
        }
    }

    /**
     * 根据个人维度规则应用飞行与移动速度.
     * 应在玩家进入维度,登录或规则变更时调用;每 tick 调用亦安全(无变化时不产生副作用).
     *
     * @param player 目标玩家
     * @param rules  维度规则
     * @return 若能力发生变化返回 true
     */
    public static boolean applyCapabilities(ServerPlayer player, PersonalDimensionRules rules) {
        Abilities abilities = player.getAbilities();
        AttributeInstance movement = movementAttribute(player);
        // 首次授予前备份原值并打标,离开维度时据此精确还原;
        // 已打标(能力由本模组授予中)时不覆盖备份,保证还原的是进入维度前的值
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(BACKUP_KEY, Tag.TAG_COMPOUND)) {
            CompoundTag backup = new CompoundTag();
            backup.putBoolean("mayfly", abilities.mayfly);
            backup.putBoolean("flying", abilities.flying);
            backup.putFloat("walkingSpeed", abilities.getWalkingSpeed());
            backup.putFloat("flyingSpeed", abilities.getFlyingSpeed());
            if (movement != null) {
                backup.putDouble(MOVEMENT_SPEED_KEY, movement.getBaseValue());
            }
            persistent.put(BACKUP_KEY, backup);
        }
        boolean changed = false;

        // 创造/旁观模式由原版强制授予飞行,规则关闭时也不收回
        boolean shouldFly = gameModeGrantsFlight(player) || rules.flightEnabled;
        if (abilities.mayfly != shouldFly) {
            abilities.mayfly = shouldFly;
            if (!shouldFly) {
                abilities.flying = false;
            }
            changed = true;
        }

        float speed = clampMovementSpeed(rules.movementSpeed);
        if (Math.abs(abilities.getFlyingSpeed() - speed) > EPSILON
                || Math.abs(abilities.getWalkingSpeed() - speed) > EPSILON) {
            abilities.setFlyingSpeed(speed);
            abilities.setWalkingSpeed(speed);
            changed = true;
        }
        if (movement != null && Math.abs(movement.getBaseValue() - (double) speed) > BASE_EPSILON) {
            movement.setBaseValue(speed);
            changed = true;
        }

        if (changed) {
            player.onUpdateAbilities();
        }

        return changed;
    }

    /**
     * 处理无飞行惯性规则:玩家停止移动输入时清零水平速度.
     * 这需要在玩家 tick 中持续调用,因为移动输入每 tick 都会变化.
     */
    public static void tickNoFlightInertia(ServerPlayer player, PersonalDimensionRules rules) {
        if (!rules.noFlightInertia) {
            return;
        }
        Abilities abilities = player.getAbilities();
        if (abilities.flying && player.zza == 0.0f && player.xxa == 0.0f) {
            player.setDeltaMovement(player.getDeltaMovement().multiply(0.0, 1.0, 0.0));
        }
    }

    /**
     * 将玩家能力还原到本模组授予前的备份值.
     *
     * <p>仅在玩家离开个人维度或下线时调用.无备份标记时说明能力并非本模组授予,
     * 不做任何改动,以免清除其他模组授予的飞行.</p>
     *
     * <p>创造与观察模式由原版强制授予的飞行不会被本次还原收回(玩家可能在维度内
     * 被切换了游戏模式),但移动速度(含属性基础值)一律回到进入维度前的数值,
     * 避免速度加成随维度切换或玩家存档泄漏出去.</p>
     *
     * <p>已知取舍:玩家在维度内被其他模组授予能力后再离开,还原则以备份为准,
     * 会覆盖该次授予;相比无条件清除,这保证"本模组未授予过就完全不动".</p>
     */
    public static void resetAbilities(ServerPlayer player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(BACKUP_KEY, Tag.TAG_COMPOUND)) {
            // 无标记:能力非本模组授予,保持现状
            return;
        }
        CompoundTag backup = persistent.getCompound(BACKUP_KEY);
        persistent.remove(BACKUP_KEY);

        Abilities abilities = player.getAbilities();
        boolean changed = false;
        boolean mayfly = backup.getBoolean("mayfly") || gameModeGrantsFlight(player);
        if (abilities.mayfly != mayfly) {
            abilities.mayfly = mayfly;
            changed = true;
        }
        // 不允许飞行时强制落地,避免离开后滞留空中飞行状态
        boolean flying = (backup.getBoolean("flying") || abilities.flying) && mayfly;
        if (abilities.flying != flying) {
            abilities.flying = flying;
            changed = true;
        }
        float walkingSpeed = backup.getFloat("walkingSpeed");
        if (Math.abs(abilities.getWalkingSpeed() - walkingSpeed) > EPSILON) {
            abilities.setWalkingSpeed(walkingSpeed);
            changed = true;
        }
        float flyingSpeed = backup.getFloat("flyingSpeed");
        if (Math.abs(abilities.getFlyingSpeed() - flyingSpeed) > EPSILON) {
            abilities.setFlyingSpeed(flyingSpeed);
            changed = true;
        }
        AttributeInstance movement = movementAttribute(player);
        if (movement != null) {
            // 旧备份没有属性基础值:按原版读取玩家存档的口径回落到 abilities.walkSpeed
            double base = backup.contains(MOVEMENT_SPEED_KEY, Tag.TAG_DOUBLE)
                    ? backup.getDouble(MOVEMENT_SPEED_KEY)
                    : (double) walkingSpeed;
            if (Math.abs(movement.getBaseValue() - base) > BASE_EPSILON) {
                movement.setBaseValue(base);
                changed = true;
            }
        }
        if (changed) {
            player.onUpdateAbilities();
        }
    }

    /**
     * 创造与旁观模式由原版强制授予飞行({@code GameType#updatePlayerAbilities}),
     * 本模组的规则不得把它们收回.
     */
    private static boolean gameModeGrantsFlight(ServerPlayer player) {
        return player.isCreative() || player.isSpectator();
    }

    /**
     * 取玩家的移动速度属性实例.属性缺失(非常规实体或测试替身)时返回 null,
     * 由调用方跳过属性写入.
     */
    @Nullable
    private static AttributeInstance movementAttribute(ServerPlayer player) {
        return player.getAttribute(Attributes.MOVEMENT_SPEED);
    }

    /**
     * 校验并限制移动速度在合理范围内,防止客户端伪造极大/极小值.
     */
    public static float clampMovementSpeed(float speed) {
        if (Float.isNaN(speed) || speed < 0.05f) {
            return 0.05f;
        }
        if (speed > 2.0f) {
            return 2.0f;
        }
        return speed;
    }
}
