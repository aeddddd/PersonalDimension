package com.github.aeddddd.personalplane.registry;

import com.github.aeddddd.personalplane.PersonalPlaneMod;
import com.github.aeddddd.personalplane.blockentity.PersonalDimensionManagerBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * 方块实体注册中心。
 */
public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> DR = DeferredRegister.create(
            Registries.BLOCK_ENTITY_TYPE, PersonalPlaneMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<PersonalDimensionManagerBlockEntity>> PERSONAL_DIMENSION_MANAGER =
            DR.register("personal_plane_manager", () -> BlockEntityType.Builder.of(
                    PersonalDimensionManagerBlockEntity::new, ModBlocks.PERSONAL_DIMENSION_MANAGER.get()).build(null));

    private ModBlockEntities() {
    }
}
