package com.github.aeddddd.personaldimension.client;

import com.github.aeddddd.personaldimension.PersonalDimensionMod;
import com.github.aeddddd.personaldimension.client.gui.PersonalDimensionCreateScreen;
import com.github.aeddddd.personaldimension.client.gui.PersonalDimensionManagerScreen;
import com.github.aeddddd.personaldimension.client.render.PersonalDimensionManagerRenderer;
import com.github.aeddddd.personaldimension.registry.ModBlockEntities;
import com.github.aeddddd.personaldimension.registry.ModMenus;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * 客户端初始化：注册菜单界面。
 */
@EventBusSubscriber(modid = PersonalDimensionMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {

    private ClientSetup() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.PERSONAL_DIMENSION_MANAGER.get(), PersonalDimensionManagerScreen::new);
        event.register(ModMenus.PERSONAL_DIMENSION_CREATE.get(), PersonalDimensionCreateScreen::new);
        BlockEntityRenderers.register(ModBlockEntities.PERSONAL_DIMENSION_MANAGER.get(),
                PersonalDimensionManagerRenderer::new);
    }
}
