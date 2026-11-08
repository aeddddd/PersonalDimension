package com.github.aeddddd.personaldimension.access;

import com.github.aeddddd.personaldimension.PersonalDimensionConfig;
import com.github.aeddddd.personaldimension.dimension.OwnerKey;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;

/**
 * FTB 在场时的访问策略：队伍关系决定权限，FTB Ranks 节点可显式授予。
 */
public final class FtbAccessPolicy implements AccessPolicy {

    private final TeamAccess teamAccess;

    public FtbAccessPolicy(TeamAccess teamAccess) {
        this.teamAccess = teamAccess;
    }

    @Override
    public boolean can(ServerPlayer player, OwnerKey owner, AccessAction action) {
        if (player instanceof FakePlayer) {
            return true;
        }
        if (owner.isPlayer() && owner.id().equals(player.getUUID())) {
            return true;
        }
        if (player.hasPermissions(PersonalDimensionConfig.COMMON.opOverrideLevel.get())) {
            return true;
        }
        if (PersonalDimensionConfig.COMMON.useFtbRanks.get()
                && RanksAccess.grants(player.getGameProfile(), node(action))) {
            return true;
        }
        if (owner.isPlayer()) {
            // 个人维度不共享给队友；只有所有者、OP 与 Ranks 可访问
            return false;
        }

        TeamAccess.Rank rank = teamAccess.rankOf(player.getUUID(), owner.id());
        return switch (action) {
            case ENTER -> rank == TeamAccess.Rank.OWNER
                    || rank == TeamAccess.Rank.OFFICER
                    || rank == TeamAccess.Rank.MEMBER
                    || (rank == TeamAccess.Rank.ALLY && PersonalDimensionConfig.COMMON.allowAllyEnter.get());
            case BUILD, INTERACT -> rank == TeamAccess.Rank.OWNER
                    || rank == TeamAccess.Rank.OFFICER
                    || rank == TeamAccess.Rank.MEMBER
                    || (rank == TeamAccess.Rank.ALLY && PersonalDimensionConfig.COMMON.allyCanBuild.get());
            case MANAGE -> rank == TeamAccess.Rank.OWNER
                    || (rank == TeamAccess.Rank.OFFICER && PersonalDimensionConfig.COMMON.officerCanManage.get());
        };
    }

    private static String node(AccessAction action) {
        return switch (action) {
            case ENTER -> "personal_dimension.enter";
            case BUILD -> "personal_dimension.build";
            case INTERACT -> "personal_dimension.interact";
            case MANAGE -> "personal_dimension.manage";
        };
    }
}
