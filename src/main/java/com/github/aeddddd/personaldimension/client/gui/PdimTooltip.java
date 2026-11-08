package com.github.aeddddd.personaldimension.client.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 提示文本的换行处理.
 *
 * <p>{@code GuiGraphics#renderTooltip(Font, Component, int, int)} 只渲染单行,组件里的
 * {@code '\n'} 会被当作普通字符交给字体,而默认字体没有该字形,于是画成一个缺字方块(豆腐块),
 * 后续文字也全部挤在同一行.所有含换行的提示都必须先拆成多行,再用
 * {@code renderComponentTooltip} 绘制.</p>
 *
 * <p>拆分按组件树遍历({@link Component#visit}),因此各段的样式(颜色/粗体等)与可变参数
 * 翻译都保持不变;连续换行会产生空行.</p>
 */
final class PdimTooltip {

    private PdimTooltip() {
    }

    /**
     * 把可能含 {@code '\n'} 的组件拆成行,至少返回一行.
     */
    static List<Component> lines(Component text) {
        List<MutableComponent> lines = new ArrayList<>();
        lines.add(Component.empty());
        text.visit((style, content) -> {
            int start = 0;
            for (int i = 0; i < content.length(); i++) {
                if (content.charAt(i) == '\n') {
                    append(lines, style, content.substring(start, i));
                    lines.add(Component.empty());
                    start = i + 1;
                }
            }
            append(lines, style, content.substring(start));
            return Optional.empty();
        }, Style.EMPTY);
        return new ArrayList<>(lines);
    }

    private static void append(List<MutableComponent> lines, Style style, String content) {
        if (!content.isEmpty()) {
            lines.get(lines.size() - 1).append(Component.literal(content).withStyle(style));
        }
    }
}
