package com.github.aeddddd.personalplane.dimension;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

/**
 * 个人维度地板布局设置(按维度所有者持久化):层数,是否启用马路,马路包围的区块数.
 *
 * <p>默认与旧版行为一致:单层,启用马路,马路包围 5×5 区块(即 6×6 区块网格的首行/首列为马路).
 * 层数把同一楼层样式按 {@link #LAYER_SPACING} 的垂直间距向上堆叠;马路设置只在内置
 * 默认组合样式({@code personal_plane:default})生效,使用自定义预设时由预设全权决定地板样式.</p>
 */
public class PersonalDimFloorLayout {

    /** 相邻两层地板的垂直间距(格). */
    public static final int LAYER_SPACING = 16;

    /** 层数硬上限;实际可用层数还受 floorY 约束,见 {@link #maxFloorCount(int)}. */
    public static final int MAX_FLOOR_COUNT = 12;

    /** 最高一层地板允许的最高 Y,给顶层留出站立空间. */
    public static final int MAX_LAYER_Y = 250;

    /** 马路包围的区块数下限(1×1 平台 + 一圈马路). */
    public static final int MIN_ROAD_CHUNKS = 1;

    /**
     * 马路包围的区块数上限.受管理器状态包预览尺寸约束:
     * 包围 N 区块时组合样式为 (N+1)×(N+1) 区块,预览状态表为 ((N+1)*16)^2 条,
     * 不得超过 PersonalDimManagerStatePacket.MAX_STATE_COUNT(65536),故 N ≤ 14.
     */
    public static final int MAX_ROAD_CHUNKS = 14;

    /** 默认包围区块数:5×5 平台 + 一圈马路,对齐旧版 6×6 网格. */
    public static final int DEFAULT_ROAD_CHUNKS = 5;

    /** 地板层数,≥ 1. */
    public int floorCount = 1;

    /** 是否启用马路(首行/首列马路 + 内部平台);关闭时整个地板为纯平台. */
    public boolean roadEnabled = true;

    /** 马路包围的区块边数(平台区域为 N×N 区块). */
    public int roadChunks = DEFAULT_ROAD_CHUNKS;

    /**
     * 指定 floorY 下允许的最大层数:最后一层不超过 {@link #MAX_LAYER_Y}.
     */
    public static int maxFloorCount(int floorY) {
        return Mth.clamp((MAX_LAYER_Y - floorY) / LAYER_SPACING + 1, 1, MAX_FLOOR_COUNT);
    }

    /**
     * 第 layer 层(0 起)地板的 Y 坐标.
     */
    public static int layerY(int floorY, int layer) {
        return floorY + layer * LAYER_SPACING;
    }

    /**
     * 按指定 floorY 钳制后的层数(不修改自身).
     */
    public int clampedFloorCount(int floorY) {
        return Mth.clamp(floorCount, 1, maxFloorCount(floorY));
    }

    /**
     * 按指定 floorY 就地钳制各字段到合法范围.
     */
    public void clamp(int floorY) {
        floorCount = clampedFloorCount(floorY);
        roadChunks = Mth.clamp(roadChunks, MIN_ROAD_CHUNKS, MAX_ROAD_CHUNKS);
    }

    public PersonalDimFloorLayout copy() {
        PersonalDimFloorLayout copy = new PersonalDimFloorLayout();
        copy.floorCount = floorCount;
        copy.roadEnabled = roadEnabled;
        copy.roadChunks = roadChunks;
        return copy;
    }

    public CompoundTag writeToNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("floorCount", floorCount);
        tag.putBoolean("roadEnabled", roadEnabled);
        tag.putInt("roadChunks", roadChunks);
        return tag;
    }

    /**
     * 从 NBT 读取;缺失字段保持默认值(旧存档兼容).读取后按硬上限钳制,
     * 与 floorY 相关的进一步钳制在使用处进行.
     */
    public void readFromNBT(CompoundTag tag) {
        floorCount = Mth.clamp(tag.getInt("floorCount"), 1, MAX_FLOOR_COUNT);
        roadEnabled = !tag.contains("roadEnabled") || tag.getBoolean("roadEnabled");
        int chunks = tag.contains("roadChunks") ? tag.getInt("roadChunks") : DEFAULT_ROAD_CHUNKS;
        roadChunks = Mth.clamp(chunks, MIN_ROAD_CHUNKS, MAX_ROAD_CHUNKS);
    }
}
