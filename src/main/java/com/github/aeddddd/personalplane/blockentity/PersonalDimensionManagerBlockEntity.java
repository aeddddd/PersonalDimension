package com.github.aeddddd.personalplane.blockentity;

import com.github.aeddddd.personalplane.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 个人维度终端的方块实体：仅用于承载上方终端渲染，无额外状态。
 */
public class PersonalDimensionManagerBlockEntity extends BlockEntity {

    /** 客户端平滑旋转角度；仅渲染使用，不持久化。 */
    public float renderYaw;
    public float renderPitch;
    public float renderRoll;

    public PersonalDimensionManagerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PERSONAL_DIMENSION_MANAGER.get(), pos, state);
    }
}
