package com.github.aeddddd.personaldimension;

import com.github.aeddddd.personaldimension.data.PersonalDimensionDataGenerators;
import com.github.aeddddd.personaldimension.dimension.PersonalDimChunkGenerator;
import com.github.aeddddd.personaldimension.dimension.PersonalDimensionManager;
import com.github.aeddddd.personaldimension.dimension.PresetLoader;
import com.github.aeddddd.personaldimension.registry.ModBlockEntities;
import com.github.aeddddd.personaldimension.registry.ModBlocks;
import com.github.aeddddd.personaldimension.registry.ModCreativeTab;
import com.github.aeddddd.personaldimension.registry.ModItems;
import com.github.aeddddd.personaldimension.registry.ModMenus;
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
 * Personal Dimension 主类。
 */
@Mod(PersonalDimensionMod.MOD_ID)
public class PersonalDimensionMod {

    public static final String MOD_ID = "personal_dimension";
    public static final Logger LOGGER = LogManager.getLogger(PersonalDimensionMod.class);

    public PersonalDimensionMod(IEventBus modEventBus, ModContainer modContainer) {
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
        LOGGER.info("Personal Dimension common setup");
        event.enqueueWork(() -> {
            PresetLoader.migrateLegacyPresetIfNeeded();
            PresetLoader.copyDefaultPresetToConfigIfMissing();
            PresetLoader.registerBuiltinDefault();

            NeoForge.EVENT_BUS.register(PersonalDimensionManager.class);
        });
    }
}
