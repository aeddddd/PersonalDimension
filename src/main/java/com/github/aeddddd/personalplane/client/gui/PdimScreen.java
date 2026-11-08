package com.github.aeddddd.personalplane.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

import javax.annotation.Nullable;

/**
 * 个人维度创建向导与管理器的公共骨架.
 *
 * <p>两个界面只在页签内容上不同,骨架完全一致:配色编辑器的事件转发(它自绘,不在子控件列表里,
 * 必须由界面手动转发),地板预览区的滚轮缩放,悬停提示渲染,以及标题改在 renderBg 里绘制.
 * 这些逻辑集中在这里,避免两处各写一遍后只改其中一处.</p>
 */
abstract class PdimScreen<M extends AbstractContainerMenu> extends AbstractContainerScreen<M> {

    /** 地板配色编辑器;仅在地板预览/颜色面板所在页签构建,未构建时为 null. */
    @Nullable
    protected PdimColorEditor colorEditor;
    /** 地板预览的采样格数,由滚轮缩放调整(少 = 放大). */
    protected int previewCells = PdimFloorPreview.DEFAULT_CELLS;

    protected PdimScreen(M menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (colorEditor != null && colorEditor.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (colorEditor != null && colorEditor.keyPressed(keyCode)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * 光标落在预览框内时按滚轮缩放采样格数.
     *
     * @param previewY 预览框相对界面顶部的 y(创建页与预设页位置不同)
     * @return 命中并消费滚轮时为 true
     */
    protected boolean zoomPreviewOnScroll(double mouseX, double mouseY, double delta, int previewY) {
        if (delta == 0.0 || !PdimStyle.isInside(mouseX, mouseY, leftPos + PdimStyle.PREVIEW_X,
                topPos + previewY, PdimStyle.PREVIEW_W, PdimStyle.PREVIEW_W)) {
            return false;
        }
        previewCells = PdimFloorPreview.cellsAfterScroll(previewCells, delta);
        return true;
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderTooltip(graphics, mouseX, mouseY);
        Component hint = hoveredHint();
        if (hint != null) {
            // 提示可能含 '\n':必须拆行渲染,否则换行符会被画成缺字方块
            graphics.renderComponentTooltip(font, PdimTooltip.lines(hint), mouseX, mouseY);
        }
    }

    /** 悬停控件的提示文本(按钮与滑条),无悬停或该控件无提示时返回 null. */
    @Nullable
    private Component hoveredHint() {
        for (var child : children()) {
            // 只认真实悬停,不能用 isHoveredOrFocused:被点过的滑条会一直持有焦点,
            // 之后悬停其他控件(如「应用布局」按钮)时仍显示该滑条的提示,错位劫持
            if (!(child instanceof AbstractWidget widget) || !widget.isHovered()) {
                continue;
            }
            if (widget instanceof PdimButton button && button.hint() != null) {
                return button.hint();
            }
            if (widget instanceof PdimSlider slider) {
                return slider.hint();
            }
        }
        return null;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // 标题在 renderBg 中绘制
    }
}
