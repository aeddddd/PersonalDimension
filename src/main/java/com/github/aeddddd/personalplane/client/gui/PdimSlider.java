package com.github.aeddddd.personalplane.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

/**
 * 用 {@code personal_plane.png} 绘制的滑条:上方为"名称 + 当前值"文字,
 * 下方为凹槽轨道 + 强调色进度条 + 可拖动旋钮.
 *
 * <p>拖动过程中通过 {@code onChange} 立即更新本地数据(数值文字与预览同步刷新),
 * 松开鼠标或键盘调整后由 {@code onSubmit} 提交到服务端.</p>
 */
class PdimSlider extends Button {

    private final double min;
    private final double max;
    private final double step;
    private final DoubleFunction<Component> format;
    private final DoubleConsumer onChange;
    private final Runnable onSubmit;
    private final Component hint;

    private double value;
    private boolean dragging;

    PdimSlider(int x, int y, int width, double min, double max, double step, double initial, Component label,
            DoubleFunction<Component> format, DoubleConsumer onChange, Runnable onSubmit, Component hint) {
        super(x, y, width, PdimStyle.SLIDER_H, label, button -> {
        }, DEFAULT_NARRATION);
        this.min = min;
        this.max = max;
        this.step = step;
        this.format = format;
        this.onChange = onChange;
        this.onSubmit = onSubmit;
        this.hint = hint;
        setValue(initial);
    }

    Component hint() {
        return this.hint;
    }

    /**
     * 是否正处于拖动状态(供 Screen 转发 mouseDragged 时判定).
     */
    boolean isHandleDragging() {
        return this.dragging;
    }

    double getValue() {
        return this.value;
    }

    /**
     * 按外部状态(服务端同步)重置当前值.
     */
    void setValue(double newValue) {
        this.value = Mth.clamp(newValue, this.min, this.max);
    }

    private double ratio() {
        return this.max > this.min ? (this.value - this.min) / (this.max - this.min) : 0.0;
    }

    private int knobX() {
        return this.getX() + (int) Math.round(ratio() * (this.width - PdimStyle.SLIDER_KNOB_W));
    }

    private void moveTo(double rawValue) {
        double snapped = this.step > 0 ? Math.round(rawValue / this.step) * this.step : rawValue;
        double clamped = Mth.clamp(snapped, this.min, this.max);
        if (clamped == this.value) {
            return;
        }
        this.value = clamped;
        this.onChange.accept(this.value);
    }

    private void moveToMouse(double mouseX) {
        moveTo(PdimSliderMath.valueAt(mouseX, this.getX(), this.width, this.min, this.max, this.step));
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        boolean highlighted = this.isHoveredOrFocused() || this.dragging;

        graphics.drawString(font, this.getMessage(), this.getX(), this.getY() + 1, PdimStyle.TEXT_MAIN, false);
        Component valueText = this.format.apply(this.value);
        graphics.drawString(font, valueText, this.getX() + this.width - font.width(valueText), this.getY() + 1,
                highlighted ? PdimStyle.TEXT_MAIN : PdimStyle.TEXT_DIM, false);

        int trackY = this.getY() + PdimStyle.SLIDER_TRACK_Y;
        PdimStyle.draw(graphics, this.getX(), trackY, PdimStyle.TRACK);
        int fillWidth = (int) Math.round(ratio() * (this.width - 2));
        if (fillWidth > 0) {
            PdimStyle.drawSub(graphics, this.getX() + 1, trackY + 1, PdimStyle.FILL, fillWidth,
                    PdimStyle.SLIDER_FILL_H);
        }
        PdimStyle.draw(graphics, knobX(), this.getY() + PdimStyle.SLIDER_KNOB_Y,
                highlighted ? PdimStyle.KNOB_HOVER : PdimStyle.KNOB);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible || !this.isValidClickButton(button) || !this.clicked(mouseX, mouseY)) {
            return false;
        }
        this.dragging = true;
        this.playDownSound(Minecraft.getInstance().getSoundManager());
        moveToMouse(mouseX);
        return true;
    }

    /**
     * 拖动调整数值;对齐原版 {@code AbstractSliderButton}:只认左键,不要求拖拽起点在旋钮上
     * (按下时已由 {@code mouseClicked} 定位).
     */
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button != 0 || !this.active || !this.visible) {
            return false;
        }
        this.dragging = true;
        moveToMouse(mouseX);
        return true;
    }

    /**
     * 结束拖动:仍在拖动时提交并复位,返回是否确实处于拖动中(幂等,可重复调用).
     */
    boolean endDrag() {
        if (!this.dragging) {
            return false;
        }
        this.dragging = false;
        this.onSubmit.run();
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return endDrag();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        double delta = Math.max(this.step, (this.max - this.min) / 20.0);
        switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_DOWN -> {
                moveTo(this.value - delta);
                this.onSubmit.run();
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_UP -> {
                moveTo(this.value + delta);
                this.onSubmit.run();
                return true;
            }
            default -> {
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        }
    }
}
