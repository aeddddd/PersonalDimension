package com.github.aeddddd.personalplane.dimension;

import com.github.aeddddd.personalplane.access.AccessPolicies;
import com.github.aeddddd.personalplane.access.TeamAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.UUID;

/**
 * 解析玩家当前应使用的维度归属。
 *
 * <p>{@link DimensionMode#PERSONAL}、FTB 缺席、未加入 party（只有个人队伍）都归一化为
 * {@link OwnerKey#player(UUID)}；只有确实处于 party 中才使用队伍维度。</p>
 */
public final class OwnerResolver {

    private OwnerResolver() {
    }

    public static OwnerKey resolve(ServerPlayer player) {
        return resolve(player.server, player.getUUID());
    }

    public static OwnerKey resolve(MinecraftServer server, UUID playerId) {
        PlayerDimState state = PersonalDimensionData.get(server).getPlayerState(playerId);
        return resolve(playerId, state.mode);
    }

    /**
     * 按给定模式解析，不读取玩家状态。用于测试与命令。
     */
    public static OwnerKey resolve(UUID playerId, DimensionMode mode) {
        if (mode == DimensionMode.PERSONAL || !AccessPolicies.ftbAvailable()) {
            return OwnerKey.player(playerId);
        }
        Optional<TeamAccess.TeamRef> team = AccessPolicies.teamAccess().teamOf(playerId);
        if (team.isEmpty() || team.get().personalTeam()) {
            return OwnerKey.player(playerId);
        }
        return OwnerKey.team(team.get().id());
    }

    public static String displayName(MinecraftServer server, OwnerKey owner) {
        if (owner.isPlayer()) {
            var player = server.getPlayerList().getPlayer(owner.id());
            if (player != null) {
                return player.getGameProfile().getName();
            }
            var cache = server.getProfileCache();
            if (cache != null) {
                var profile = cache.get(owner.id());
                if (profile.isPresent()) {
                    return profile.get().getName();
                }
            }
            return owner.id().toString();
        }
        String name = AccessPolicies.teamAccess().teamName(owner.id());
        return name == null || name.isEmpty() ? owner.id().toString() : name;
    }

    public static int memberCount(OwnerKey owner) {
        return owner.isTeam() ? AccessPolicies.teamAccess().memberCount(owner.id()) : 1;
    }
}
