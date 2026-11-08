package com.github.aeddddd.personaldimension.registry;

import com.github.aeddddd.personaldimension.PersonalDimensionMod;
import com.github.aeddddd.personaldimension.block.CautionBlock;
import com.github.aeddddd.personaldimension.block.PersonalDimensionManagerBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * 方块注册中心。
 */
public final class ModBlocks {

    public static final DeferredRegister<Block> DR = DeferredRegister.create(Registries.BLOCK,
            PersonalDimensionMod.MOD_ID);

    public static final RegistryObject<Block> PERSONAL_DIMENSION_MANAGER = DR.register(
            "personal_dimension_manager", () -> new PersonalDimensionManagerBlock(terminalProperties()));

    /**
     * 终端渲染用的隐藏方块：不放置、不掉落、无碰撞，只提供 blockstate/model 给 BER 渲染上方终端。
     */
    public static final RegistryObject<Block> PERSONAL_DIMENSION_TERMINAL = DR.register(
            "personal_dimension_terminal", () -> new Block(hiddenTerminalProperties()));

    public static final RegistryObject<Block> CAUTION_BLOCK = DR.register("caution_block", CautionBlock::new);

    private ModBlocks() {
    }

    private static BlockBehaviour.Properties terminalProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .sound(SoundType.METAL)
                .strength(5.0F, 3600000.0F)
                .requiresCorrectToolForDrops()
                .noOcclusion()
                .isSuffocating((state, level, pos) -> false)
                .isViewBlocking((state, level, pos) -> false);
    }

    private static BlockBehaviour.Properties hiddenTerminalProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .sound(SoundType.METAL)
                .strength(-1.0F, 3600000.0F)
                .noCollission()
                .noOcclusion()
                .noLootTable()
                .isValidSpawn((state, level, pos, type) -> false)
                .isSuffocating((state, level, pos) -> false)
                .isViewBlocking((state, level, pos) -> false);
    }
}
