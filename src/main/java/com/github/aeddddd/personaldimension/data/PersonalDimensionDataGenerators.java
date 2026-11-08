package com.github.aeddddd.personaldimension.data;

import com.github.aeddddd.personaldimension.PersonalDimensionMod;
import com.github.aeddddd.personaldimension.data.client.PersonalDimensionBlockStateProvider;
import com.github.aeddddd.personaldimension.data.client.PersonalDimensionItemModelProvider;
import com.github.aeddddd.personaldimension.registry.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 数据生成入口。
 */
public final class PersonalDimensionDataGenerators {

    private PersonalDimensionDataGenerators() {
    }

    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        generator.addProvider(event.includeClient(),
                new PersonalDimensionBlockStateProvider(output, existingFileHelper));
        generator.addProvider(event.includeClient(),
                new PersonalDimensionItemModelProvider(output, existingFileHelper));

        BlockTagsProvider blockTags = new BlockTagsProvider(output, lookupProvider,
                PersonalDimensionMod.MOD_ID, existingFileHelper) {
            @Override
            protected void addTags(HolderLookup.Provider provider) {
                for (DeferredHolder<Block, ? extends Block> block : ModBlocks.DR.getEntries()) {
                    if (block.get() == ModBlocks.PERSONAL_DIMENSION_TERMINAL.get()) {
                        continue;
                    }
                    tag(BlockTags.MINEABLE_WITH_PICKAXE).add(block.get());
                }
                tag(BlockTags.NEEDS_STONE_TOOL).add(ModBlocks.CAUTION_BLOCK.get());
            }
        };
        generator.addProvider(event.includeServer(), blockTags);
        generator.addProvider(event.includeServer(), new LootTableProvider(output, Collections.emptySet(),
                List.of(new LootTableProvider.SubProviderEntry(
                        PersonalDimensionBlockLootProvider::new, LootContextParamSets.BLOCK)),
                lookupProvider));
        generator.addProvider(event.includeServer(), new PersonalDimensionRecipeProvider(output, lookupProvider));
    }
}
