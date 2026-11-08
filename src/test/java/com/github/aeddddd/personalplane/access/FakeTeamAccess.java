package com.github.aeddddd.personalplane.access;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 测试用 {@link TeamAccess} 假实现。
 */
public final class FakeTeamAccess implements TeamAccess {

    public final Map<UUID, TeamRef> teams = new HashMap<>();
    public final Map<UUID, Map<UUID, Rank>> ranks = new HashMap<>();

    public void addTeam(UUID player, TeamRef team) {
        teams.put(player, team);
    }

    public void setRank(UUID teamId, UUID player, Rank rank) {
        ranks.computeIfAbsent(teamId, key -> new HashMap<>()).put(player, rank);
    }

    @Override
    public Optional<TeamRef> teamOf(UUID playerId) {
        return Optional.ofNullable(teams.get(playerId));
    }

    @Override
    public Rank rankOf(UUID playerId, UUID teamId) {
        return ranks.getOrDefault(teamId, Map.of()).getOrDefault(playerId, Rank.NONE);
    }

    @Override
    public String teamName(UUID teamId) {
        return "team-" + teamId;
    }

    @Override
    public int memberCount(UUID teamId) {
        return ranks.getOrDefault(teamId, Map.of()).size();
    }

    @Override
    public boolean teamExists(UUID teamId) {
        return ranks.containsKey(teamId);
    }
}
