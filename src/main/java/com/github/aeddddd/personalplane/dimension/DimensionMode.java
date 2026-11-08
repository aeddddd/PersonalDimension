package com.github.aeddddd.personalplane.dimension;

/**
 * 玩家的维度模式。
 *
 * <p>{@link #TEAM} 为默认值：处于 FTB 队伍中时使用队伍共享维度，单人时归一化为个人维度。
 * {@link #PERSONAL} 强制使用玩家自己的个人维度，即使身处队伍中。FTB 缺席时该设置被忽略。</p>
 */
public enum DimensionMode {
    TEAM,
    PERSONAL
}
