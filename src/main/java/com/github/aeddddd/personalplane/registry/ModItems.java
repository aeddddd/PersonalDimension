package com.github.aeddddd.personalplane.registry;

import com.github.aeddddd.personalplane.PersonalPlaneMod;
import com.github.aeddddd.personalplane.item.PersonalDimensionCoreItem;
import com.github.aeddddd.personalplane.item.PersonalDimensionManagerItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 物品注册中心。
 */
public final class ModItems {

    public static final DeferredRegister<Item> DR = DeferredRegister.create(Registries.ITEM,
            PersonalPlaneMod.MOD_ID);

    public static final DeferredHolder<Item, Item> PERSONAL_DIMENSION = DR.register("personal_plane",
            () -> new PersonalDimensionCoreItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, Item> PERSONAL_DIMENSION_MANAGER = DR.register("personal_plane_manager",
            () -> new PersonalDimensionManagerItem(ModBlocks.PERSONAL_DIMENSION_MANAGER.get(), new Item.Properties()));

    public static final DeferredHolder<Item, Item> CAUTION_BLOCK = DR.register("caution_block",
            () -> new BlockItem(ModBlocks.CAUTION_BLOCK.get(), new Item.Properties()));

    private ModItems() {
    }
}
