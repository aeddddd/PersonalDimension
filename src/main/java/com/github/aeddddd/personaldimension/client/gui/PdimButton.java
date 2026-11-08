package com.github.aeddddd.personaldimension.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

/**
 * 用 {@code personal_dimension.png} 绘制的按钮:开关状态,选中状态与悬停都用纹理表现,
 * 文字只作为标签叠加,避免原版按钮与 mod 内其它 GUI 风格不一致.
 */
class PdimButton extends Button {

    private final PdimStyle.Sprite normal;
    private final PdimStyle.Sprite hovered;
    private final int textColor;
    @Nullable
    private final Component hint;

    private PdimButton(int x, int y, int width, int height, PdimStyle.Sprite normal, PdimStyle.Sprite hovered,
            int textColor, Component label, @Nullable Component hint, OnPress onPress) {
        super(x, y, width, height, label, onPress, DEFAULT_NARRATION);
        this.normal = normal;
        this.hovered = hovered;
        this.textColor = textColor;
        this.hint = hint;
    }

    /**
     * 开关按钮:开启态整体使用强调色底,便于一眼区分状态.
     */
    static PdimButton toggle(int x, int y, boolean on, Component label, @Nullable Component hint, OnPress onPress) {
        return new PdimButton(x, y, PdimStyle.TOGGLE_W, PdimStyle.TOGGLE_H,
                on ? PdimStyle.TOGGLE_ON : PdimStyle.TOGGLE_OFF,
                on ? PdimStyle.TOGGLE_ON_HOVER : PdimStyle.TOGGLE_OFF_HOVER,
                PdimStyle.TEXT_MAIN, label, hint, onPress);
    }

    static PdimButton tab(int x, int y, boolean selected, Component label, OnPress onPress) {
        return new PdimButton(x, y, PdimStyle.TAB_SM_W, PdimStyle.TAB_H,
                selected ? PdimStyle.TAB_SM_ON : PdimStyle.TAB_SM_NORMAL,
                selected ? PdimStyle.TAB_SM_ON : PdimStyle.TAB_SM_HOVER,
                PdimStyle.TEXT_MAIN, label, null, onPress);
    }

    static PdimButton action(int x, int y, Component label, @Nullable Component hint, OnPress onPress) {
        return new PdimButton(x, y, PdimStyle.ACTION.w(), PdimStyle.ACTION.h(),
                PdimStyle.ACTION, PdimStyle.ACTION_HOVER, PdimStyle.TEXT_MAIN, label, hint, onPress);
    }

    static PdimButton small(int x, int y, Component label, @Nullable Component hint, OnPress onPress) {
        return new PdimButton(x, y, PdimStyle.SMALL.w(), PdimStyle.SMALL.h(),
                PdimStyle.SMALL, PdimStyle.SMALL_HOVER, PdimStyle.TEXT_MAIN, label, hint, onPress);
    }

    @Nullable
    Component hint() {
        return this.hint;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PdimStyle.Sprite sprite;
        if (!this.active) {
            sprite = this.width == PdimStyle.ACTION_DISABLED.w() && this.height == PdimStyle.ACTION_DISABLED.h()
                    ? PdimStyle.ACTION_DISABLED
                    : this.normal;
        } else {
            sprite = this.isHoveredOrFocused() ? this.hovered : this.normal;
        }
        PdimStyle.draw(graphics, this.getX(), this.getY(), sprite);
        int color = this.active ? this.textColor : PdimStyle.TEXT_MUTED;
        PdimStyle.drawCenteredText(graphics, Minecraft.getInstance().font, this.getMessage(),
                this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, color);
    }
}
