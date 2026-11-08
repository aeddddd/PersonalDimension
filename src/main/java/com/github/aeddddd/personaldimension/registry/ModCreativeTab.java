package com.github.aeddddd.personaldimension.registry;

import com.github.aeddddd.personaldimension.PersonalDimensionMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * 创造模式物品栏。
 */
public final class ModCreativeTab {

    public static final DeferredRegister<CreativeModeTab> DR = DeferredRegister.create(Registries.CREATIVE_MODE_TAB,
            PersonalDimensionMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = DR.register("personal_dimension",
            () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                    .title(Component.translatable("itemGroup.personal_dimension"))
                    .icon(() -> new ItemStack(ModItems.PERSONAL_DIMENSION.get()))
                    .displayItems((params, output) -> {
                        output.accept(new ItemStack(ModItems.PERSONAL_DIMENSION.get()));
                        output.accept(new ItemStack(ModItems.PERSONAL_DIMENSION_MANAGER.get()));
                        output.accept(new ItemStack(ModItems.CAUTION_BLOCK.get()));
                    })
                    .build());

    private ModCreativeTab() {
    }
}
