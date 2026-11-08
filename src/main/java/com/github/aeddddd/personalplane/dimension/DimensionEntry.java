package com.github.aeddddd.personalplane.dimension;

import com.github.aeddddd.personalplane.api.dimension.FloorColorScheme;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * 单个维度的持久化数据（按 {@link OwnerKey} 存储）。
 *
 * <p>队伍维度由全队共享：规则、配色、布局、入口点与时间/天气都保存在这里。
 * 玩家的返回点与模式保存在 {@link PlayerDimState}，不随维度共享。</p>
 */
public class DimensionEntry {

    public boolean created = false;
    public PersonalDimensionRules rules = new PersonalDimensionRules();
    public BlockPos entryPoint = new BlockPos(0, 65, 0);
    public FloorColorScheme colorScheme = FloorColorScheme.createDefault();
    public PersonalDimFloorLayout floorLayout = new PersonalDimFloorLayout();

    public boolean hasStoredLevelData = false;
    public long dayTime;
    public int clearWeatherTime;
    public int rainTime;
    public int thunderTime;
    public boolean raining;
    public boolean thundering;

    public CompoundTag writeToNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("created", created);
        tag.put("rules", rules.writeToNBT());
        tag.putLong("entryPoint", entryPoint.asLong());
        tag.put("colorScheme", colorScheme.writeToNBT());
        tag.put("floorLayout", floorLayout.writeToNBT());
        if (hasStoredLevelData) {
            tag.putLong("dayTime", dayTime);
            tag.putInt("clearWeatherTime", clearWeatherTime);
            tag.putInt("rainTime", rainTime);
            tag.putInt("thunderTime", thunderTime);
            tag.putBoolean("raining", raining);
            tag.putBoolean("thundering", thundering);
        }
        return tag;
    }

    public void readFromNBT(CompoundTag tag) {
        created = tag.getBoolean("created");
        rules.readFromNBT(tag.getCompound("rules"));
        entryPoint = BlockPos.of(tag.getLong("entryPoint"));
        if (tag.contains("colorScheme", Tag.TAG_COMPOUND)) {
            colorScheme.readFromNBT(tag.getCompound("colorScheme"));
        }
        if (tag.contains("floorLayout", Tag.TAG_COMPOUND)) {
            floorLayout.readFromNBT(tag.getCompound("floorLayout"));
        }
        hasStoredLevelData = tag.contains("dayTime", Tag.TAG_LONG);
        if (hasStoredLevelData) {
            dayTime = tag.getLong("dayTime");
            clearWeatherTime = tag.getInt("clearWeatherTime");
            rainTime = tag.getInt("rainTime");
            thunderTime = tag.getInt("thunderTime");
            raining = tag.getBoolean("raining");
            thundering = tag.getBoolean("thundering");
        }
    }
}
