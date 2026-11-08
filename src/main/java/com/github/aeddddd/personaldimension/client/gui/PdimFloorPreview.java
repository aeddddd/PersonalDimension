package com.github.aeddddd.personaldimension.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 个人维度地板样式预览:直接把样式里的方块以俯视图方式绘制成缩略图.
 *
 * <p>预览取方块的实际粒子纹理({@code getParticleIcon}),比纯色块更接近成品效果;
 * 三个颜色角色(路面/标线/平台)的占位方块按当前编辑中的染料色替换后再取纹理.</p>
 *
 * <p>采样格数({@code cellBudget})决定"看到多大范围":格数越少单格越大(放大),越多越接近整体缩略图,
 * 由 GUI 的滚轮缩放调节(见 {@link #cellsAfterScroll}).</p>
 */
final class PdimFloorPreview {

    /** 各轴采样格数:默认值/可调上下限(上限兼顾清晰度与每帧绘制量). */
    static final int DEFAULT_CELLS = 32;
    static final int MIN_CELLS = 8;
    static final int MAX_CELLS = 48;
    /** 外框内缩:1px 外描边 + 1px 内斜面. */
    private static final int INSET = 2;
    /** 滚轮一格改变的采样格数. */
    private static final int CELL_STEP = 4;
    /** 预设以 16x16 为可平铺单元,预览按此画出参考网格线. */
    private static final int TILE = 16;
    private static final int TILE_LINE_COLOR = 0x402F3B47;

    private PdimFloorPreview() {
    }

    /**
     * 滚轮一格后的采样格数:向上滚(delta > 0)放大,即减少格数;向下滚缩小.
     */
    static int cellsAfterScroll(int current, double delta) {
        int clamped = Math.max(MIN_CELLS, Math.min(MAX_CELLS, current));
        if (delta == 0.0) {
            return clamped;
        }
        int next = clamped - (int) Math.signum(delta) * CELL_STEP;
        return Math.max(MIN_CELLS, Math.min(MAX_CELLS, next));
    }

    /**
     * 在指定位置绘制预览(外框 {@link PdimStyle#PREVIEW_W} x {@link PdimStyle#PREVIEW_W}).
     *
     * @param cellBudget 较长轴的采样格数,超出上下限时钳制
     */
    static void render(GuiGraphics graphics, int x, int y, int width, int depth, int[] states, List<String> palette,
            DyeColor road, DyeColor line, DyeColor platform, int cellBudget) {
        PdimStyle.draw(graphics, x, y, PdimStyle.PREVIEW_FRAME);
        if (width <= 0 || depth <= 0 || states == null || states.length == 0 || palette.isEmpty()) {
            return;
        }

        int inner = PdimStyle.PREVIEW_W - INSET * 2;
        int frameX = x + INSET;
        int frameY = y + INSET;
        int budget = Math.max(MIN_CELLS, Math.min(MAX_CELLS, cellBudget));
        int step = Math.max(1, (int) Math.ceil(Math.max(width, depth) / (double) budget));
        int cellsX = (width + step - 1) / step;
        int cellsZ = (depth + step - 1) / step;
        float cell = (float) inner / Math.max(cellsX, cellsZ);

        BlockState[] previewStates = new BlockState[palette.size()];
        for (int i = 0; i < palette.size(); i++) {
            previewStates[i] = resolveState(palette.get(i), road, line, platform);
        }

        graphics.enableScissor(frameX, frameY, frameX + inner, frameY + inner);
        for (int cz = 0; cz < cellsZ; cz++) {
            for (int cx = 0; cx < cellsX; cx++) {
                int srcX = Math.min(width - 1, cx * step);
                int srcZ = Math.min(depth - 1, cz * step);
                int index = srcZ * width + srcX;
                if (index < 0 || index >= states.length) {
                    continue;
                }
                int paletteIndex = states[index];
                if (paletteIndex < 0 || paletteIndex >= previewStates.length) {
                    continue;
                }
                BlockState state = previewStates[paletteIndex];
                if (state != null && state.isAir()) {
                    // 未定义/空气格子在预览里留空(与生成器「该格不铺方块」同口径)
                    continue;
                }
                int x1 = frameX + Math.round(cx * cell);
                int y1 = frameY + Math.round(cz * cell);
                int x2 = Math.max(x1 + 1, frameX + Math.round((cx + 1) * cell));
                int y2 = Math.max(y1 + 1, frameY + Math.round((cz + 1) * cell));
                if (state != null) {
                    graphics.blit(x1, y1, 0, x2 - x1, y2 - y1, Minecraft.getInstance().getBlockRenderer()
                            .getBlockModelShaper().getParticleIcon(state));
                } else {
                    graphics.fill(x1, y1, x2, y2, 0xFF000000 | resolveMapColor(palette.get(paletteIndex)));
                }
            }
        }
        drawTileGrid(graphics, frameX, frameY, inner, cell, step, width, depth);
        graphics.disableScissor();
    }

    /**
     * 按 16 格单元画参考网格线,过密时自动省略.
     */
    private static void drawTileGrid(GuiGraphics graphics, int frameX, int frameY, int inner, float cell, int step,
            int width, int depth) {
        float unit = TILE / (float) step * cell;
        if (unit < 10.0f) {
            return;
        }
        for (int x = TILE; x < width; x += TILE) {
            int lineX = frameX + Math.round(x / (float) step * cell);
            graphics.fill(lineX, frameY, lineX + 1, frameY + inner, TILE_LINE_COLOR);
        }
        for (int z = TILE; z < depth; z += TILE) {
            int lineY = frameY + Math.round(z / (float) step * cell);
            graphics.fill(frameX, lineY, frameX + inner, lineY + 1, TILE_LINE_COLOR);
        }
    }

    /**
     * 解析样式条目对应的方块状态:三个颜色角色的占位方块替换为当前染料色混凝土.
     */
    private static BlockState resolveState(String blockName, DyeColor road, DyeColor line, DyeColor platform) {
        DyeColor override = switch (blockName) {
            case "minecraft:gray_concrete" -> road;
            case "minecraft:white_concrete" -> line;
            case "minecraft:black_concrete" -> platform;
            default -> null;
        };
        String resolved = override != null ? "minecraft:" + override.getName() + "_concrete" : blockName;
        ResourceLocation id = ResourceLocation.tryParse(resolved);
        if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) {
            return null;
        }
        return BuiltInRegistries.BLOCK.get(id).defaultBlockState();
    }

    /**
     * 染料色混凝土的显示色,用于调色板色块.
     */
    static int concreteColor(DyeColor color) {
        ResourceLocation id = new ResourceLocation("minecraft", color.getName() + "_concrete");
        if (BuiltInRegistries.BLOCK.containsKey(id)) {
            return BuiltInRegistries.BLOCK.get(id).defaultMapColor().col & 0xFFFFFF;
        }
        return 0x7F7F7F;
    }

    /**
     * 方块注册名对应的地图色,方块无法解析时退回中性灰.
     */
    static int resolveMapColor(String blockName) {
        try {
            ResourceLocation id = ResourceLocation.tryParse(blockName);
            if (id != null && BuiltInRegistries.BLOCK.containsKey(id)) {
                return BuiltInRegistries.BLOCK.get(id).defaultMapColor().col & 0xFFFFFF;
            }
        } catch (Exception ignored) {
        }
        return 0x7F7F7F;
    }
}
