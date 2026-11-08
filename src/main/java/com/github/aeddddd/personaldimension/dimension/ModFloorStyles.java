package com.github.aeddddd.personaldimension.dimension;

import com.github.aeddddd.personaldimension.PersonalDimensionMod;
import com.github.aeddddd.personaldimension.api.dimension.IFloorPreset;
import com.github.aeddddd.personaldimension.api.dimension.IFloorTile;
import com.github.aeddddd.personaldimension.api.dimension.PersonalDimensionApi;
import com.github.aeddddd.personaldimension.registry.ModBlocks;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内置地板样式:与主分支 96×96 预设一致,以 6×6 区块网格的组合样式表达,
 * 第一行与第一列为马路(11 种边框单元),其余 5×5 为平台(同一种单元).
 */
public final class ModFloorStyles {

    /** 组合样式网格边长(区块),96 格 / 16 = 6. */
    public static final int GRID_SIZE = 6;

    /** 平台区域边长(区块),占据网格的 5×5. */
    public static final int PLATFORM_SIZE = 5;

    /**
     * 按布局构建的组合样式缓存.key: 关闭马路为 0,否则为包围区块数.
     * 样式不可变且只引用注册过的单元,缓存可跨存档保留.
     */
    private static final Map<Integer, FloorStyle> STYLE_CACHE = new ConcurrentHashMap<>();
    /** 已注册的马路/平台区块单元与默认组合样式,由 {@link #register()} 填充. */
    private static volatile FloorPreset roadTile;
    private static volatile FloorPreset topRoadTile;
    private static volatile FloorPreset leftRoadTile;
    private static volatile FloorPreset cornerTile;
    private static volatile FloorPreset platformTile;
    private static volatile FloorStyle defaultStyle;

    private ModFloorStyles() {
    }

    /**
     * 注册内置区块单元(马路/平台)与默认组合样式;id 已被其他模组占用时不覆盖.
     */
    public static void register() {
        if (defaultStyle != null) {
            // 幂等:重复调用不重建样式实例,避免 API 注册表/样式缓存与静态引用漂移
            return;
        }
        FloorPreset road;
        FloorPreset topRoad;
        FloorPreset leftRoad;
        FloorPreset corner;
        FloorPreset platform;
        FloorStyle style;

        FloorPreset source = PresetLoader.loadBuiltinAsset();
        if (source != null && source.width == GRID_SIZE * IFloorTile.SIZE
                && source.depth == GRID_SIZE * IFloorTile.SIZE) {
            // 与主分支逐格一致:将 96×96 预设切为 6×6 个区块单元拼合
            FloorPreset[] tiles = sliceIntoTiles(source);
            // 首行/首列不是同一种路块:转角、上边、左边各自保留预设里的方向变体,
            // 否则自定义包围区块数时会把竖直路块当成水平路块铺到首行.
            corner = tiles[tileIndex(0, 0)];
            topRoad = tiles[tileIndex(2, 0)];
            leftRoad = tiles[tileIndex(0, 2)];
            road = leftRoad;
            platform = tiles[tileIndex(1, 1)];
            style = new FloorStyle(GRID_SIZE, GRID_SIZE, tiles);
        } else {
            // 内置 asset 缺失或尺寸异常时兜底:近似主分支风格的马路+平台组合
            PersonalDimensionMod.LOGGER.warn("[AE2E] Builtin floor preset asset missing or has unexpected size, "
                    + "using simplified road/platform fallback style.");
            road = createRoadTile();
            topRoad = road;
            leftRoad = road;
            corner = road;
            platform = createPlatformTile();
            style = createFallbackStyle(road, platform);
        }

        roadTile = road;
        topRoadTile = topRoad;
        leftRoadTile = leftRoad;
        cornerTile = corner;
        platformTile = platform;
        defaultStyle = style;

        if (PersonalDimensionApi.getFloorTile(PersonalDimensionApi.ROAD_TILE_ID).isEmpty()) {
            PersonalDimensionApi.registerFloorTile(PersonalDimensionApi.ROAD_TILE_ID, road);
        }
        if (PersonalDimensionApi.getFloorTile(PersonalDimensionApi.PLATFORM_TILE_ID).isEmpty()) {
            PersonalDimensionApi.registerFloorTile(PersonalDimensionApi.PLATFORM_TILE_ID, platform);
        }
        if (PersonalDimensionApi.getFloorPreset(PersonalDimensionApi.DEFAULT_PRESET_ID).isEmpty()) {
            PersonalDimensionApi.registerFloorPreset(PersonalDimensionApi.DEFAULT_PRESET_ID, style);
        }
    }

    private static int tileIndex(int chunkX, int chunkZ) {
        return chunkZ * GRID_SIZE + chunkX;
    }

    /**
     * 默认组合样式(register 后可用);未注册或 id 被占用时仍返回本地构建的样式实例.
     */
    @Nullable
    public static FloorStyle defaultStyle() {
        return defaultStyle;
    }

    /**
     * 该预设是否为内置默认组合样式(马路布局设置只对它生效).
     */
    public static boolean isBuiltinStyle(IFloorPreset preset) {
        return defaultStyle != null && preset == defaultStyle;
    }

    /**
     * 按布局设置解析实际生效的地板样式:仅当 base 是内置默认组合样式时,
     * 马路开关与包围区块数才生效(自定义预设由预设全权决定地板样式);否则原样返回 base.
     */
    public static IFloorPreset applyLayout(IFloorPreset base, @Nullable PersonalDimFloorLayout layout) {
        FloorStyle builtin = defaultStyle;
        if (layout == null || builtin == null || base != builtin) {
            return base;
        }
        return styleFor(layout.roadEnabled, layout.roadChunks);
    }

    /**
     * 按布局构建组合样式:关闭马路时为纯平台(1×1 网格);开启时为 (N+1)×(N+1) 网格,
     * 首行/首列为马路,内部 N×N 为平台.N 等于默认包围数时直接复用默认组合样式,
     * 保留内置预设切片中的全部边框变体(转角/直段等).
     */
    public static FloorStyle styleFor(boolean roadEnabled, int roadChunks) {
        int key = roadEnabled
                ? Mth.clamp(roadChunks, PersonalDimFloorLayout.MIN_ROAD_CHUNKS, PersonalDimFloorLayout.MAX_ROAD_CHUNKS)
                : 0;
        return STYLE_CACHE.computeIfAbsent(key, ModFloorStyles::buildStyle);
    }

    private static FloorStyle buildStyle(int key) {
        FloorPreset fallbackRoad = roadTile != null ? roadTile : createRoadTile();
        FloorPreset corner = cornerTile != null ? cornerTile : fallbackRoad;
        FloorPreset top = topRoadTile != null ? topRoadTile : fallbackRoad;
        FloorPreset left = leftRoadTile != null ? leftRoadTile : fallbackRoad;
        FloorPreset platform = platformTile != null ? platformTile : createPlatformTile();
        if (key == 0) {
            return FloorStyle.filled(1, 1, platform);
        }
        FloorStyle builtin = defaultStyle;
        if (key + 1 == GRID_SIZE && builtin != null) {
            return builtin;
        }
        FloorStyle.Builder builder = new FloorStyle.Builder(key + 1, key + 1, platform);
        builder.set(0, 0, corner);
        for (int i = 1; i <= key; i++) {
            builder.set(i, 0, top);
            builder.set(0, i, left);
        }
        return builder.build();
    }

    /**
     * 将 96×96 预设按 16×16 切为 6×6 个区块单元(行优先).
     */
    private static FloorPreset[] sliceIntoTiles(FloorPreset source) {
        FloorPreset[] tiles = new FloorPreset[GRID_SIZE * GRID_SIZE];
        for (int tz = 0; tz < GRID_SIZE; tz++) {
            for (int tx = 0; tx < GRID_SIZE; tx++) {
                List<BlockState> palette = new ArrayList<>();
                Map<BlockState, Integer> paletteIndex = new LinkedHashMap<>();
                int[] states = new int[IFloorTile.SIZE * IFloorTile.SIZE];
                for (int z = 0; z < IFloorTile.SIZE; z++) {
                    for (int x = 0; x < IFloorTile.SIZE; x++) {
                        BlockState state = source.getState(tx * IFloorTile.SIZE + x, tz * IFloorTile.SIZE + z);
                        if (state == null) {
                            // 与生成器同口径:未定义格子留空,不用基岩兜底
                            state = Blocks.AIR.defaultBlockState();
                        }
                        Integer idx = paletteIndex.get(state);
                        if (idx == null) {
                            idx = palette.size();
                            palette.add(state);
                            paletteIndex.put(state, idx);
                        }
                        states[z * IFloorTile.SIZE + x] = idx;
                    }
                }
                tiles[tileIndex(tx, tz)] = new FloorPreset(IFloorTile.SIZE, IFloorTile.SIZE,
                        palette.toArray(new BlockState[0]), states);
            }
        }
        return tiles;
    }

    /**
     * 兜底组合样式:第一行与第一列为马路,其余 5×5 为平台.
     */
    private static FloorStyle createFallbackStyle(FloorPreset road, FloorPreset platform) {
        FloorStyle.Builder builder = new FloorStyle.Builder(GRID_SIZE, GRID_SIZE, platform);
        builder.set(0, 0, road);
        for (int i = 1; i < GRID_SIZE; i++) {
            builder.set(i, 0, road);
            builder.set(0, i, road);
        }
        return builder.build();
    }

    /**
     * 兜底马路单元:灰色混凝土路面 + 白色混凝土十字中线 + 外圈警示方块路缘.
     */
    private static FloorPreset createRoadTile() {
        BlockState stripes = ModBlocks.CAUTION_BLOCK.get().defaultBlockState();
        BlockState asphalt = Blocks.GRAY_CONCRETE.defaultBlockState();
        BlockState line = Blocks.WHITE_CONCRETE.defaultBlockState();
        BlockState[] palette = { stripes, asphalt, line };
        int[] states = new int[IFloorTile.SIZE * IFloorTile.SIZE];
        int mid = IFloorTile.SIZE / 2;
        for (int z = 0; z < IFloorTile.SIZE; z++) {
            for (int x = 0; x < IFloorTile.SIZE; x++) {
                boolean edge = x == 0 || z == 0 || x == IFloorTile.SIZE - 1 || z == IFloorTile.SIZE - 1;
                boolean centerLine = x == mid - 1 || x == mid || z == mid - 1 || z == mid;
                states[z * IFloorTile.SIZE + x] = edge ? 0 : (centerLine ? 2 : 1);
            }
        }
        return new FloorPreset(IFloorTile.SIZE, IFloorTile.SIZE, palette, states);
    }

    /**
     * 兜底平台单元:纯黑色混凝土,与主分支中心广场一致.
     */
    private static FloorPreset createPlatformTile() {
        BlockState[] palette = { Blocks.BLACK_CONCRETE.defaultBlockState() };
        int[] states = new int[IFloorTile.SIZE * IFloorTile.SIZE];
        return new FloorPreset(IFloorTile.SIZE, IFloorTile.SIZE, palette, states);
    }
}
