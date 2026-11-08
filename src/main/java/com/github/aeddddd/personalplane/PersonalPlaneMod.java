package com.github.aeddddd.personalplane;

import com.github.aeddddd.personalplane.data.PersonalDimensionDataGenerators;
import com.github.aeddddd.personalplane.dimension.PersonalDimChunkGenerator;
import com.github.aeddddd.personalplane.dimension.PersonalDimensionManager;
import com.github.aeddddd.personalplane.dimension.PresetLoader;
import com.github.aeddddd.personalplane.registry.ModBlockEntities;
import com.github.aeddddd.personalplane.registry.ModBlocks;
import com.github.aeddddd.personalplane.registry.ModCreativeTab;
import com.github.aeddddd.personalplane.registry.ModItems;
import com.github.aeddddd.personalplane.registry.ModMenus;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Personal Plane 主类。
 */
@Mod(PersonalPlaneMod.MOD_ID)
public class PersonalPlaneMod {

    public static final String MOD_ID = "personal_plane";
    public static final Logger LOGGER = LogManager.getLogger(PersonalPlaneMod.class);

    public PersonalPlaneMod(IEventBus modEventBus, ModContainer modContainer) {
        PersonalDimensionConfig.register(modContainer);

        ModBlocks.DR.register(modEventBus);
        ModBlockEntities.DR.register(modEventBus);
        ModItems.DR.register(modEventBus);
        ModMenus.DR.register(modEventBus);
        ModCreativeTab.DR.register(modEventBus);

        modEventBus.addListener(this::registerChunkGenerator);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(PersonalDimensionDataGenerators::gatherData);
    }

    /**
     * 区块生成器的 codec 必须在注册阶段登记，Minecraft 1.21 的注册表在公共初始化前已冻结。
     */
    private void registerChunkGenerator(RegisterEvent event) {
        event.register(Registries.CHUNK_GENERATOR,
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "personal_dim"),
                () -> PersonalDimChunkGenerator.CODEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Personal Plane common setup");
        event.enqueueWork(() -> {
            PresetLoader.migrateLegacyPresetIfNeeded();
            PresetLoader.copyDefaultPresetToConfigIfMissing();
            PresetLoader.registerBuiltinDefault();

            NeoForge.EVENT_BUS.register(PersonalDimensionManager.class);
        });
    }
}
