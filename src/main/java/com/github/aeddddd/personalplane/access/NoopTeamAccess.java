package com.github.aeddddd.personalplane.access;

import java.util.Optional;
import java.util.UUID;

/**
 * FTB 缺席时使用的空实现。
 */
public final class NoopTeamAccess implements TeamAccess {

    public static final NoopTeamAccess INSTANCE = new NoopTeamAccess();

    private NoopTeamAccess() {
    }

    @Override
    public Optional<TeamRef> teamOf(UUID playerId) {
        return Optional.empty();
    }

    @Override
    public Rank rankOf(UUID playerId, UUID teamId) {
        return Rank.UNKNOWN;
    }

    @Override
    public String teamName(UUID teamId) {
        return "";
    }

    @Override
    public int memberCount(UUID teamId) {
        return 0;
    }

    @Override
    public boolean teamExists(UUID teamId) {
        return false;
    }
}
