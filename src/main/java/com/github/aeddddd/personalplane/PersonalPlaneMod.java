package com.github.aeddddd.personalplane;

import com.github.aeddddd.personalplane.data.PersonalDimensionDataGenerators;
import com.github.aeddddd.personalplane.dimension.PersonalDimChunkGenerator;
import com.github.aeddddd.personalplane.dimension.PersonalDimensionManager;
import com.github.aeddddd.personalplane.dimension.PresetLoader;
import com.github.aeddddd.personalplane.network.ModNetwork;
import com.github.aeddddd.personalplane.registry.ModBlockEntities;
import com.github.aeddddd.personalplane.registry.ModBlocks;
import com.github.aeddddd.personalplane.registry.ModCreativeTab;
import com.github.aeddddd.personalplane.registry.ModItems;
import com.github.aeddddd.personalplane.registry.ModMenus;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Personal Plane 主类。
 */
@Mod(PersonalPlaneMod.MOD_ID)
public class PersonalPlaneMod {

    public static final String MOD_ID = "personal_plane";
    public static final Logger LOGGER = LogManager.getLogger(PersonalPlaneMod.class);

    public PersonalPlaneMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        PersonalDimensionConfig.register();

        ModBlocks.DR.register(modEventBus);
        ModBlockEntities.DR.register(modEventBus);
        ModItems.DR.register(modEventBus);
        ModMenus.DR.register(modEventBus);
        ModCreativeTab.DR.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(PersonalDimensionDataGenerators::gatherData);

        ModNetwork.init();
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Personal Plane common setup");
        event.enqueueWork(() -> {
            Registry.register(BuiltInRegistries.CHUNK_GENERATOR,
                    new ResourceLocation(MOD_ID, "personal_dim"), PersonalDimChunkGenerator.CODEC.codec());
            PresetLoader.migrateLegacyPresetIfNeeded();
            PresetLoader.copyDefaultPresetToConfigIfMissing();
            PresetLoader.registerBuiltinDefault();

            MinecraftForge.EVENT_BUS.register(PersonalDimensionManager.class);
        });
    }
}
