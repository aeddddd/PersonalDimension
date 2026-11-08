package com.github.aeddddd.personaldimension;

import com.github.aeddddd.personaldimension.data.PersonalDimensionDataGenerators;
import com.github.aeddddd.personaldimension.dimension.PersonalDimChunkGenerator;
import com.github.aeddddd.personaldimension.dimension.PersonalDimensionManager;
import com.github.aeddddd.personaldimension.dimension.PresetLoader;
import com.github.aeddddd.personaldimension.network.ModNetwork;
import com.github.aeddddd.personaldimension.registry.ModBlockEntities;
import com.github.aeddddd.personaldimension.registry.ModBlocks;
import com.github.aeddddd.personaldimension.registry.ModCreativeTab;
import com.github.aeddddd.personaldimension.registry.ModItems;
import com.github.aeddddd.personaldimension.registry.ModMenus;
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
 * Personal Dimension 主类。
 */
@Mod(PersonalDimensionMod.MOD_ID)
public class PersonalDimensionMod {

    public static final String MOD_ID = "personal_dimension";
    public static final Logger LOGGER = LogManager.getLogger(PersonalDimensionMod.class);

    public PersonalDimensionMod() {
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
        LOGGER.info("Personal Dimension common setup");
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
