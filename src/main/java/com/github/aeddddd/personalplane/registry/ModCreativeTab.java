package com.github.aeddddd.personalplane.registry;

import com.github.aeddddd.personalplane.PersonalPlaneMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * 创造模式物品栏。
 */
public final class ModCreativeTab {

    public static final DeferredRegister<CreativeModeTab> DR = DeferredRegister.create(Registries.CREATIVE_MODE_TAB,
            PersonalPlaneMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> TAB = DR.register("personal_plane",
            () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                    .title(Component.translatable("itemGroup.personal_plane"))
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
