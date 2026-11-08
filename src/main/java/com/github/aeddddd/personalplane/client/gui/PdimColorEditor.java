package com.github.aeddddd.personalplane.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import org.lwjgl.glfw.GLFW;

/**
 * 地板颜色方案编辑器:三行"角色 + 色块",点击色块弹出 16 色调色板直接选色.
 *
 * <p>相比逐次点击循环 16 种颜色,弹出式调色板一次即可选中目标颜色,行与弹窗均自绘,
 * 不占用 Screen 的子控件列表.</p>
 */
final class PdimColorEditor {

    private static final String[] LABEL_KEYS = {
            "gui.personal_plane.pdim.color.road_base",
            "gui.personal_plane.pdim.color.road_line",
            "gui.personal_plane.pdim.color.platform_base"
    };

    private static final int POPUP_COLS = 4;
    private static final int POPUP_CELL = 16;
    private static final int POPUP_GAP = 2;
    private static final int POPUP_PAD = 4;
    private static final int BORDER_COLOR = 0xFF414354;
    private static final int POPUP_FILL = 0xFFC6C7D0;

    private final int x;
    private final int y;
    private final int panelX;
    private final int panelY;
    private final int panelW;
    private final int panelH;

    private final DyeColor[] colors = { DyeColor.GRAY, DyeColor.WHITE, DyeColor.BLACK };
    private int openRow = -1;
    private int popupX;
    private int popupY;

    PdimColorEditor(int x, int y, int panelX, int panelY, int panelW, int panelH) {
        this.x = x;
        this.y = y;
        this.panelX = panelX;
        this.panelY = panelY;
        this.panelW = panelW;
        this.panelH = panelH;
    }

    /** 第 row 行相对行区顶部的偏移. */
    static int rowOffset(int row) {
        return row * (PdimStyle.COLOR_ROW_H + PdimStyle.COLOR_ROW_GAP);
    }

    private static void drawSwatch(GuiGraphics graphics, int swatchX, int swatchY, DyeColor color) {
        int size = PdimStyle.COLOR_SWATCH;
        graphics.fill(swatchX - 1, swatchY - 1, swatchX + size + 1, swatchY + size + 1, BORDER_COLOR);
        graphics.fill(swatchX, swatchY, swatchX + size, swatchY + size,
                0xFF000000 | PdimFloorPreview.concreteColor(color));
    }

    private static int popupWidth() {
        return POPUP_PAD * 2 + POPUP_COLS * POPUP_CELL + (POPUP_COLS - 1) * POPUP_GAP;
    }

    void setColors(int road, int line, int platform) {
        colors[0] = DyeColor.byId(road);
        colors[1] = DyeColor.byId(line);
        colors[2] = DyeColor.byId(platform);
        openRow = -1;
    }

    DyeColor road() {
        return colors[0];
    }

    DyeColor line() {
        return colors[1];
    }

    DyeColor platform() {
        return colors[2];
    }

    boolean isPopupOpen() {
        return openRow >= 0;
    }

    void render(GuiGraphics graphics, int mouseX, int mouseY) {
        Font font = Minecraft.getInstance().font;
        for (int i = 0; i < colors.length; i++) {
            int rowY = this.y + rowOffset(i);
            boolean hovered = PdimStyle.isInside(mouseX, mouseY, this.x, rowY, PdimStyle.COLOR_ROW_W,
                    PdimStyle.COLOR_ROW_H) && openRow != i;
            PdimStyle.draw(graphics, this.x, rowY, hovered ? PdimStyle.COLOR_ROW_HOVER : PdimStyle.COLOR_ROW);
            graphics.drawString(font, Component.translatable(LABEL_KEYS[i]), this.x + 6, rowY + 5,
                    PdimStyle.TEXT_MAIN, false);
            drawSwatch(graphics, this.x + PdimStyle.COLOR_ROW_W - PdimStyle.COLOR_SWATCH - 5, rowY + 3, colors[i]);
        }
        if (openRow >= 0) {
            drawPopup(graphics, mouseX, mouseY);
        }
    }

    private void openPopup(int row) {
        int width = popupWidth();
        int rowY = this.y + rowOffset(row);
        int targetX = this.x + PdimStyle.COLOR_ROW_W - width;
        int targetY = rowY + PdimStyle.COLOR_ROW_H + 2;
        if (targetY + width > this.panelY + this.panelH - 4) {
            // 下方放不下时向上展开
            targetY = rowY - width - 2;
        }
        this.popupX = Mth.clamp(targetX, this.panelX + 2, this.panelX + this.panelW - width - 2);
        this.popupY = Mth.clamp(targetY, this.panelY + 2, this.panelY + this.panelH - width - 2);
        this.openRow = row;
    }

    private void drawPopup(GuiGraphics graphics, int mouseX, int mouseY) {
        int width = popupWidth();
        graphics.fill(popupX, popupY, popupX + width, popupY + width, BORDER_COLOR);
        graphics.fill(popupX + 1, popupY + 1, popupX + width - 1, popupY + width - 1, POPUP_FILL);
        DyeColor current = colors[openRow];
        for (int i = 0; i < 16; i++) {
            DyeColor color = DyeColor.byId(i);
            int cellX = popupX + POPUP_PAD + (i % POPUP_COLS) * (POPUP_CELL + POPUP_GAP);
            int cellY = popupY + POPUP_PAD + (i / POPUP_COLS) * (POPUP_CELL + POPUP_GAP);
            boolean hovered = PdimStyle.isInside(mouseX, mouseY, cellX, cellY, POPUP_CELL, POPUP_CELL);
            if (hovered) {
                graphics.fill(cellX - 1, cellY - 1, cellX + POPUP_CELL + 1, cellY + POPUP_CELL + 1, 0xFFFFFFFF);
            } else if (color == current) {
                graphics.fill(cellX - 1, cellY - 1, cellX + POPUP_CELL + 1, cellY + POPUP_CELL + 1, BORDER_COLOR);
            }
            graphics.fill(cellX, cellY, cellX + POPUP_CELL, cellY + POPUP_CELL,
                    0xFF000000 | PdimFloorPreview.concreteColor(color));
        }
    }

    private int popupCellAt(double mouseX, double mouseY) {
        for (int i = 0; i < 16; i++) {
            int cellX = popupX + POPUP_PAD + (i % POPUP_COLS) * (POPUP_CELL + POPUP_GAP);
            int cellY = popupY + POPUP_PAD + (i / POPUP_COLS) * (POPUP_CELL + POPUP_GAP);
            if (PdimStyle.isInside(mouseX, mouseY, cellX, cellY, POPUP_CELL, POPUP_CELL)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 处理点击,返回是否消费该事件.
     */
    boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        int previouslyOpen = openRow;
        if (previouslyOpen >= 0) {
            int width = popupWidth();
            if (PdimStyle.isInside(mouseX, mouseY, popupX, popupY, width, width)) {
                int cell = popupCellAt(mouseX, mouseY);
                if (cell >= 0) {
                    colors[previouslyOpen] = DyeColor.byId(cell);
                    openRow = -1;
                }
                return true;
            }
            // 点到弹窗之外:先收起,再按普通点击继续处理
            openRow = -1;
        }
        for (int i = 0; i < colors.length; i++) {
            if (PdimStyle.isInside(mouseX, mouseY, x, y + rowOffset(i), PdimStyle.COLOR_ROW_W,
                    PdimStyle.COLOR_ROW_H)) {
                if (previouslyOpen == i) {
                    return true;
                }
                openPopup(i);
                return true;
            }
        }
        return false;
    }

    /**
     * 处理按键,返回是否消费该事件.
     */
    boolean keyPressed(int keyCode) {
        if (openRow >= 0 && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            openRow = -1;
            return true;
        }
        return false;
    }
}
