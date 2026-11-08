package com.github.aeddddd.personalplane.dimension;

import net.minecraft.nbt.CompoundTag;

/**
 * 单个玩家的持久化状态：维度模式与返回点。
 *
 * <p>返回点按玩家保存，因此访客进入队伍维度时不会覆盖其他成员的返回点。</p>
 */
public class PlayerDimState {

    public DimensionMode mode = DimensionMode.TEAM;

    public String returnDim = "minecraft:overworld";
    public double returnX, returnY, returnZ;
    public float returnYaw, returnPitch;
    public boolean hasReturnPoint = false;

    public CompoundTag writeToNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("mode", mode.name());
        tag.putString("returnDim", returnDim);
        tag.putDouble("returnX", returnX);
        tag.putDouble("returnY", returnY);
        tag.putDouble("returnZ", returnZ);
        tag.putFloat("returnYaw", returnYaw);
        tag.putFloat("returnPitch", returnPitch);
        tag.putBoolean("hasReturnPoint", hasReturnPoint);
        return tag;
    }

    public void readFromNBT(CompoundTag tag) {
        try {
            mode = DimensionMode.valueOf(tag.getString("mode"));
        } catch (IllegalArgumentException e) {
            mode = DimensionMode.TEAM;
        }
        returnDim = tag.getString("returnDim");
        if (returnDim.isEmpty()) {
            returnDim = "minecraft:overworld";
        }
        returnX = tag.getDouble("returnX");
        returnY = tag.getDouble("returnY");
        returnZ = tag.getDouble("returnZ");
        returnYaw = tag.getFloat("returnYaw");
        returnPitch = tag.getFloat("returnPitch");
        hasReturnPoint = tag.getBoolean("hasReturnPoint");
    }
}
