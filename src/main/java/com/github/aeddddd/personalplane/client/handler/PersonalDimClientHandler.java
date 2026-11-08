package com.github.aeddddd.personalplane.client.handler;

import com.github.aeddddd.personalplane.PersonalPlaneMod;
import com.github.aeddddd.personalplane.client.ClientPersonalDimRules;
import com.github.aeddddd.personalplane.dimension.PersonalDimensionManager;
import com.github.aeddddd.personalplane.dimension.PersonalDimensionRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

/**
 * 个人维度的客户端规则执行.
 *
 * <p>服务端无法获知非骑乘玩家的移动输入({@code zza}/{@code xxa} 仅在骑乘时通过
 * {@code ServerboundPlayerInputPacket} 更新),且飞行移动为客户端权威,
 * 服务端清零 deltaMovement 不会影响客户端,因此"无飞行惯性"必须在客户端执行.</p>
 */
@EventBusSubscriber(modid = PersonalPlaneMod.MOD_ID, value = Dist.CLIENT)
public final class PersonalDimClientHandler {

    private PersonalDimClientHandler() {
    }

    /**
     * 无飞行惯性:飞行时无移动输入即清零水平速度,停止漂移.
     *
     * <p>执行前先按客户端自己看到的维度二次确认:规则缓存来自服务端同步包,
     * 任何一次同步缺失(传送事件被其他模组拦截,缓存未及时清空等)都会让该规则
     * 在主世界等非个人维度里继续生效,因此这里以"客户端当前所处维度"为准兜底.</p>
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !PersonalDimensionManager.isPersonalDimension(player.level().dimension())) {
            return;
        }
        PersonalDimensionRules rules = ClientPersonalDimRules.getCurrent();
        if (rules == null || !rules.noFlightInertia) {
            return;
        }
        if (player.getAbilities().flying && player.zza == 0.0f && player.xxa == 0.0f) {
            player.setDeltaMovement(player.getDeltaMovement().multiply(0.0, 1.0, 0.0));
        }
    }

    /**
     * 退出服务器时清空规则缓存,避免残留到下一个存档.
     */
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientPersonalDimRules.update(null);
    }
}
