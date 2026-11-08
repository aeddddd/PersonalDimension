package com.github.aeddddd.personalplane.dimension;

import com.github.aeddddd.personalplane.api.dimension.FloorColorScheme;
import com.github.aeddddd.personalplane.api.dimension.IFloorPreset;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 个人维度 ChunkGenerator:空世界 + 按预设单元平铺地板.
 *
 * <p>地板下方不再生成任何垫层:样式未定义的格子留空(空世界默认空气),破坏地板后会直接落入虚空.</p>
 */
public class PersonalDimChunkGenerator extends ChunkGenerator {

    public static final MapCodec<PersonalDimChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(generator -> generator.biomeSource),
                    Codec.INT.optionalFieldOf("floor_y", 64).forGetter(generator -> generator.floorY))
            .apply(instance, PersonalDimChunkGenerator::new));

    private final int floorY;

    public PersonalDimChunkGenerator(BiomeSource biomeSource, int floorY) {
        super(biomeSource);
        this.floorY = floorY;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    /**
     * 获取生成目标维度的所有者颜色方案;非个人维度或无条目时返回 null.
     */
    @Nullable
    private static FloorColorScheme resolveColorScheme(ServerLevel level) {
        DimensionEntry entry = resolveEntry(level);
        return entry != null ? entry.colorScheme : null;
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState random, BiomeManager biomeManager,
            StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving step) {
        // 无雕刻
    }

    /**
     * 获取生成目标维度的所有者地板布局;非个人维度或无条目时返回 null.
     */
    @Nullable
    private static PersonalDimFloorLayout resolveLayout(ServerLevel level) {
        DimensionEntry entry = resolveEntry(level);
        return entry != null ? entry.floorLayout : null;
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
        // 个人维度不生成任何生物
    }

    @Nullable
    private static DimensionEntry resolveEntry(ServerLevel level) {
        if (!PersonalDimensionManager.isPersonalDimension(level.dimension())) {
            return null;
        }
        return PersonalDimensionManager.getEntryByDimension(level.getServer(), level.dimension());
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState random,
            StructureManager structureManager, ChunkAccess chunk) {
        // 空世界:不填充任何噪声地形
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState random,
            ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        ServerLevel serverLevel = level.getLevel();
        // 按维度所有者的布局设置解析样式(马路开关/包围区块数)与层数;非个人维度保持默认
        PersonalDimFloorLayout layout = resolveLayout(serverLevel);
        IFloorPreset preset = ModFloorStyles.applyLayout(PresetLoader.getPreset(), layout);
        int floors = layout != null ? layout.clampedFloorCount(floorY) : 1;
        // 按维度所有者的颜色方案替换样式占位色
        FloorColorScheme scheme = resolveColorScheme(serverLevel);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int layer = 0; layer < floors; layer++) {
            int layerY = PersonalDimFloorLayout.layerY(floorY, layer);
            for (int lx = 0; lx < 16; lx++) {
                for (int lz = 0; lz < 16; lz++) {
                    int worldX = chunkPos.getBlockX(lx);
                    int worldZ = chunkPos.getBlockZ(lz);
                    BlockState state = preset.getState(worldX, worldZ);
                    if (state == null) {
                        // 样式未定义该格:留空(空世界默认就是空气),不再用基岩兜底
                        state = Blocks.AIR.defaultBlockState();
                    }
                    if (scheme != null) {
                        state = scheme.apply(state);
                    }
                    chunk.setBlockState(pos.set(lx, layerY, lz), state, false);
                }
            }
        }
    }

    // ==================== 禁止地物/结构/生物自然生成(对齐 1.12 主分支) ====================

    /**
     * 禁止结构起点生成,对齐 1.12 的 generateStructures() = false;
     * 不覆写时基类会按平原群系匹配村庄/前哨站/矿井/要塞等原版结构.
     */
    @Override
    public void createStructures(RegistryAccess registryAccess, ChunkGeneratorStructureState state,
            StructureManager structureManager, ChunkAccess chunk, StructureTemplateManager templateManager) {
        // 个人维度不生成任何结构
    }

    @Override
    public void createReferences(WorldGenLevel level, StructureManager structureManager, ChunkAccess chunk) {
        // 无结构引用
    }

    @Override
    @Nullable
    public Pair<BlockPos, Holder<Structure>> findNearestMapStructure(ServerLevel level, HolderSet<Structure> structure,
            BlockPos pos, int searchRadius, boolean skipKnownStructures) {
        // 结构定位(如 /locate)在个人维度永远无结果
        return null;
    }

    /**
     * 禁止地物(树木/草/花/矿石等 biome decoration)自然生成,对齐 1.12 的空 populate
     * 与 MixinGameRegistry 对 IWorldGenerator 的拦截(Forge 的 BiomeModifier 同样走此入口).
     */
    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        // 个人维度不生成任何地物或装饰
    }

    /**
     * 禁止任何类别生物自然生成,对齐 1.12 的 getPossibleCreatures() 恒空;
     * 不覆写时平原群系的动物(牛/羊/猪/鸡/马)会自然刷出.
     */
    @Override
    public WeightedRandomList<MobSpawnSettings.SpawnerData> getMobsAt(Holder<Biome> biome,
            StructureManager structureManager, MobCategory category, BlockPos pos) {
        return WeightedRandomList.create();
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    @Deprecated
    public int getGenDepth() {
        return 256;
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        return floorY + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        return new NoiseColumn(level.getMinBuildHeight(), new BlockState[0]);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
    }

    @Override
    public int getSpawnHeight(LevelHeightAccessor level) {
        return floorY + 1;
    }

    @Override
    public int getFirstFreeHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        return floorY + 1;
    }

    @Override
    public int getFirstOccupiedHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level,
            RandomState random) {
        return floorY;
    }
}
