package com.github.aeddddd.personaldimension.client;

import com.github.aeddddd.personaldimension.PersonalDimensionMod;
import com.github.aeddddd.personaldimension.client.gui.PersonalDimensionCreateScreen;
import com.github.aeddddd.personaldimension.client.gui.PersonalDimensionManagerScreen;
import com.github.aeddddd.personaldimension.client.render.PersonalDimensionManagerRenderer;
import com.github.aeddddd.personaldimension.registry.ModBlockEntities;
import com.github.aeddddd.personaldimension.registry.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 客户端初始化：注册菜单界面。
 */
@Mod.EventBusSubscriber(modid = PersonalDimensionMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {

    private ClientSetup() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.PERSONAL_DIMENSION_MANAGER.get(),
                    PersonalDimensionManagerScreen::new);
            MenuScreens.register(ModMenus.PERSONAL_DIMENSION_CREATE.get(),
                    PersonalDimensionCreateScreen::new);
            BlockEntityRenderers.register(ModBlockEntities.PERSONAL_DIMENSION_MANAGER.get(),
                    PersonalDimensionManagerRenderer::new);
        });
    }
}
