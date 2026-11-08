package com.github.aeddddd.personaldimension.registry;

import com.github.aeddddd.personaldimension.PersonalDimensionMod;
import com.github.aeddddd.personaldimension.item.PersonalDimensionCoreItem;
import com.github.aeddddd.personaldimension.item.PersonalDimensionManagerItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * 物品注册中心。
 */
public final class ModItems {

    public static final DeferredRegister<Item> DR = DeferredRegister.create(Registries.ITEM,
            PersonalDimensionMod.MOD_ID);

    public static final RegistryObject<Item> PERSONAL_DIMENSION = DR.register("personal_dimension",
            () -> new PersonalDimensionCoreItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> PERSONAL_DIMENSION_MANAGER = DR.register("personal_dimension_manager",
            () -> new PersonalDimensionManagerItem(ModBlocks.PERSONAL_DIMENSION_MANAGER.get(), new Item.Properties()));

    public static final RegistryObject<Item> CAUTION_BLOCK = DR.register("caution_block",
            () -> new BlockItem(ModBlocks.CAUTION_BLOCK.get(), new Item.Properties()));

    private ModItems() {
    }
}
