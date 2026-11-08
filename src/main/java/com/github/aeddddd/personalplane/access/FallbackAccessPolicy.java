package com.github.aeddddd.personalplane.access;

import com.github.aeddddd.personalplane.PersonalDimensionConfig;
import com.github.aeddddd.personalplane.dimension.OwnerKey;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;

/**
 * FTB 缺席时的退化策略：仅维度所有者与 OP 可用。
 *
 * <p>此时不存在队伍维度；若存档里仍有队伍维度（例如服务器移除 FTB），只允许 OP 操作，
 * 不再猜测队伍关系。</p>
 */
public final class FallbackAccessPolicy implements AccessPolicy {

    public static final FallbackAccessPolicy INSTANCE = new FallbackAccessPolicy();

    private FallbackAccessPolicy() {
    }

    @Override
    public boolean can(ServerPlayer player, OwnerKey owner, AccessAction action) {
        if (player instanceof FakePlayer) {
            return true;
        }
        if (owner.isPlayer() && owner.id().equals(player.getUUID())) {
            return true;
        }
        return player.hasPermissions(PersonalDimensionConfig.COMMON.opOverrideLevel.get());
    }
}
