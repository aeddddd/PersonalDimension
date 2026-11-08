package com.github.aeddddd.personalplane.access;

import javax.annotation.Nullable;

/**
 * 运行时选择访问策略：FTB Teams 在场时使用 {@link FtbAccessPolicy}，否则使用
 * {@link FallbackAccessPolicy}。选择结果在首次查询时缓存；模组列表在运行期不变。
 */
public final class AccessPolicies {

    @Nullable
    private static volatile AccessPolicy current;
    @Nullable
    private static volatile TeamAccess teamAccess;
    private static volatile boolean testOverride;

    private AccessPolicies() {
    }

    public static boolean ftbAvailable() {
        return testOverride || FtbTeamAccess.available();
    }

    public static boolean ranksAvailable() {
        return RanksAccess.available();
    }

    public static TeamAccess teamAccess() {
        if (!ftbAvailable()) {
            return NoopTeamAccess.INSTANCE;
        }
        TeamAccess access = teamAccess;
        if (access == null) {
            access = new FtbTeamAccess();
            teamAccess = access;
        }
        return access;
    }

    public static AccessPolicy current() {
        AccessPolicy policy = current;
        if (policy == null) {
            policy = ftbAvailable() ? new FtbAccessPolicy(teamAccess()) : FallbackAccessPolicy.INSTANCE;
            current = policy;
        }
        return policy;
    }

    /**
     * 测试用：清空缓存，让下一次查询重新选择。
     */
    public static void reset() {
        current = null;
        teamAccess = null;
        testOverride = false;
    }

    /**
     * 测试用：强制使用给定的队伍访问实现，模拟 FTB 在场。
     */
    public static void overrideTeamAccessForTest(TeamAccess access) {
        teamAccess = access;
        current = new FtbAccessPolicy(access);
        testOverride = true;
    }
}
