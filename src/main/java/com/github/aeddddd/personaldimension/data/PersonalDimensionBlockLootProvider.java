package com.github.aeddddd.personaldimension.data;

import com.github.aeddddd.personaldimension.registry.ModBlocks;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

import java.util.Set;
import java.util.stream.StreamSupport;

/**
 * 方块战利品表数据生成器。
 */
public class PersonalDimensionBlockLootProvider extends BlockLootSubProvider {

    public PersonalDimensionBlockLootProvider() {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags());
    }

    @Override
    protected void generate() {
        dropSelf(ModBlocks.PERSONAL_DIMENSION_MANAGER.get());
        dropSelf(ModBlocks.CAUTION_BLOCK.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return StreamSupport.stream(ModBlocks.DR.getEntries().spliterator(), false)
                .map(entry -> entry.get())
                .filter(block -> block != ModBlocks.PERSONAL_DIMENSION_TERMINAL.get())::iterator;
    }
}
