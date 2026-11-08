package com.github.aeddddd.personalplane.client.gui;

/**
 * 滑条取值换算:把鼠标横坐标映射为滑条值.
 *
 * <p>点击定位与拖动共用同一套换算(对齐原版 {@code AbstractSliderButton#setValueFromMouse} 的语义):
 * 旋钮中心对齐鼠标,可拖动区间是滑条两端各内缩半个旋钮宽,超出两端一律钳制到 min/max,
 * {@code step > 0} 时按 step 取整.</p>
 */
final class PdimSliderMath {

    /** 旋钮宽度(与 KNOB 精灵同宽). */
    static final int KNOB_W = 10;

    private PdimSliderMath() {
    }

    /**
     * 计算鼠标横坐标对应的滑条值.
     *
     * @param mouseX 鼠标横坐标(与滑条同一坐标系)
     * @param x      滑条左沿
     * @param width  滑条宽度
     * @param min    最小值
     * @param max    最大值
     * @param step   步长,{@code <= 0} 表示连续取值
     */
    static double valueAt(double mouseX, int x, int width, double min, double max, double step) {
        if (max <= min) {
            return min;
        }
        double usable = Math.max(1.0, width - KNOB_W);
        double ratio = (mouseX - x - KNOB_W / 2.0) / usable;
        double raw = min + clamp(ratio, 0.0, 1.0) * (max - min);
        double snapped = step > 0 ? Math.round(raw / step) * step : raw;
        return clamp(snapped, min, max);
    }

    private static double clamp(double value, double min, double max) {
        return value < min ? min : (value > max ? max : value);
    }
}
