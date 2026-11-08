package com.github.aeddddd.personalplane.client.gui;

/**
 * 滚动条滑块的几何换算:滑块高度按「可视行数 / 总条目数」缩放(下限 {@link #MIN_THUMB_H}),
 * 滑块位置按滚动进度线性映射到轨道剩余长度.
 *
 * <p>通用内存卡列表与个人维度权限页列表共用同一套公式;两处调用方都只在
 * 「条目数 &gt; 可视行数」时才使用本类——条目不足时滑块铺满轨道,不需要换算.</p>
 */
final class ScrollBarMath {

    /** 滑块最小高度,防止条目极多时滑块细到看不清. */
    static final int MIN_THUMB_H = 16;

    private ScrollBarMath() {
    }

    /** 滑块高度 = 轨道高 × 可视行数 / 总条目数,下限 {@link #MIN_THUMB_H}. */
    static int thumbHeight(int trackH, int visibleRows, int total) {
        return Math.max(MIN_THUMB_H, trackH * visibleRows / total);
    }

    /**
     * 滑块顶 = 轨道顶 + 滚动进度 × (轨道高 - 滑块高) / 最大滚动位.
     *
     * @param trackY 轨道顶(与 {@code scroll} 同一坐标系)
     * @param scroll 当前滚动位
     * @param trackH 轨道高
     * @param thumbH 滑块高(由 {@link #thumbHeight} 求得)
     * @param maxScroll 最大滚动位(总条目数 - 可视行数,须大于 0)
     */
    static int thumbY(int trackY, int scroll, int trackH, int thumbH, int maxScroll) {
        return trackY + scroll * (trackH - thumbH) / maxScroll;
    }
}
