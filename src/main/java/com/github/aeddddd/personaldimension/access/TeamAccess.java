package com.github.aeddddd.personaldimension.access;

import java.util.Optional;
import java.util.UUID;

/**
 * FTB 队伍的抽象访问层。实现可以是 FTB Teams 反射桥接，也可以是测试替身。
 */
public interface TeamAccess {

    /**
     * 队伍快照。
     *
     * @param id           队伍 id（{@code Team#getTeamId()}）
     * @param name         显示名
     * @param personalTeam 是否为玩家的个人队伍（未加入 party）
     */
    record TeamRef(UUID id, String name, boolean personalTeam) {
    }

    enum Rank {
        OWNER,
        OFFICER,
        MEMBER,
        ALLY,
        INVITED,
        NONE,
        UNKNOWN
    }

    /**
     * 查询玩家当前所属队伍。FTB 缺席或查询失败时返回空。
     */
    Optional<TeamRef> teamOf(UUID playerId);

    /**
     * 查询玩家在指定队伍中的等级。
     */
    Rank rankOf(UUID playerId, UUID teamId);

    /**
     * 队伍显示名；未知队伍返回空字符串。
     */
    String teamName(UUID teamId);

    /**
     * 队伍成员数；未知队伍返回 0。
     */
    int memberCount(UUID teamId);

    /**
     * 队伍是否仍然存在。
     */
    boolean teamExists(UUID teamId);
}
