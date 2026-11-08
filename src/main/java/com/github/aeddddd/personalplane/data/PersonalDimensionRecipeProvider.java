package com.github.aeddddd.personalplane.data;

import com.github.aeddddd.personalplane.PersonalPlaneMod;
import com.github.aeddddd.personalplane.registry.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.concurrent.CompletableFuture;

/**
 * 配方数据生成器。
 */
public class PersonalDimensionRecipeProvider extends RecipeProvider {

    private static final TagKey<Item> CONCRETE = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(PersonalPlaneMod.MOD_ID, "concrete"));

    public PersonalDimensionRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        // 核心由管理器与木棍无序合成，管理器作为合成剩余物保留。
        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, ModItems.PERSONAL_DIMENSION.get())
                .requires(ModItems.PERSONAL_DIMENSION_MANAGER.get())
                .requires(Items.STICK)
                .unlockedBy("has_manager", has(ModItems.PERSONAL_DIMENSION_MANAGER.get()))
                .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModItems.PERSONAL_DIMENSION_MANAGER.get())
                .pattern("OCO")
                .pattern("CEC")
                .pattern("OCO")
                .define('O', Items.OBSIDIAN)
                .define('C', CONCRETE)
                .define('E', Items.ENDER_PEARL)
                .unlockedBy("has_ender_pearl", has(Items.ENDER_PEARL))
                .save(output);
    }
}
