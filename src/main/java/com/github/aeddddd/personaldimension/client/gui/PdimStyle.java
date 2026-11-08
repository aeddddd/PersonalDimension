package com.github.aeddddd.personaldimension.client.gui;

import com.github.aeddddd.personaldimension.PersonalDimensionMod;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * 个人维度 GUI 的纹理坐标,配色与布局常量.
 *
 * <p>纹理 {@code personal_dimension.png} 与 mod 内其它 GUI 纹理({@code singularity_gui.png},
 * {@code umc_gui.png},{@code me_omni_tool_gui.png})共用同一套配色:浅灰面板,深色描边,
 * 蓝灰内凹区,青色强调色.所有精灵均按 1:1 像素取样,不做拉伸.</p>
 *
 * <p>纹理由 {@code research/texgen/gen_pdim_gui.py} 生成:全部控件与面板都由
 * {@code research/texgen/ae2gui.py} 的基础块九宫格拉伸拼接,同一状态的所有尺寸结构一致,
 * 状态只靠配色区分(普通灰 / 悬停提亮 / 开启蓝 / 禁用深灰).面板底部收边与
 * {@code singularity_gui.png} 母题一致(高光线 + 2px 弱化阴影 + 1px 深色描边,
 * 见 {@code test/resource/PdimGuiTextureTest} 的像素断言).</p>
 */
final class PdimStyle {

    private PdimStyle() {
    }

    static final ResourceLocation TEXTURE = new ResourceLocation(PersonalDimensionMod.MOD_ID,
            "textures/gui/personal_dimension.png");

    /** 纹理边长,作为 blit 的取样基准. */
    static final int TEX_SIZE = 512;

    /**
     * 纹理内的一块精灵区域.
     */
    record Sprite(int u, int v, int w, int h) {
    }

    // ==================== 面板 ====================

    static final int PANEL_W = 240;
    static final int PANEL_H = 220;
    static final int CREATE_PANEL_H = 176;

    static final Sprite PANEL_MANAGER = new Sprite(0, 0, PANEL_W, PANEL_H);
    static final Sprite PANEL_CREATE = new Sprite(0, 224, PANEL_W, CREATE_PANEL_H);

    // ==================== 控件精灵 ====================

    // 页签(4 页签并排,54px 窄版)
    static final Sprite TAB_SM_NORMAL = new Sprite(336, 0, 54, 18);
    static final Sprite TAB_SM_HOVER = new Sprite(336, 20, 54, 18);
    static final Sprite TAB_SM_ON = new Sprite(336, 40, 54, 18);

    static final Sprite TOGGLE_OFF = new Sprite(256, 64, 110, 18);
    static final Sprite TOGGLE_OFF_HOVER = new Sprite(256, 84, 110, 18);
    static final Sprite TOGGLE_ON = new Sprite(256, 104, 110, 18);
    static final Sprite TOGGLE_ON_HOVER = new Sprite(256, 124, 110, 18);

    static final Sprite ACTION = new Sprite(256, 148, 100, 20);
    static final Sprite ACTION_HOVER = new Sprite(256, 170, 100, 20);
    static final Sprite ACTION_DISABLED = new Sprite(256, 192, 100, 20);

    static final Sprite ROW = new Sprite(256, 216, 224, 20);
    static final Sprite ROW_HOVER = new Sprite(256, 238, 224, 20);

    static final Sprite INSET_INPUT = new Sprite(256, 260, 176, 18);

    static final Sprite PREVIEW_FRAME = new Sprite(256, 282, 100, 100);

    static final Sprite TRACK = new Sprite(256, 386, 224, 8);
    static final Sprite FILL = new Sprite(256, 396, 224, 8);
    static final Sprite KNOB = new Sprite(256, 406, 10, 14);
    static final Sprite KNOB_HOVER = new Sprite(256, 422, 10, 14);

    static final Sprite COLOR_ROW = new Sprite(256, 440, 118, 18);
    static final Sprite COLOR_ROW_HOVER = new Sprite(256, 460, 118, 18);

    static final Sprite SMALL = new Sprite(444, 80, 42, 18);
    static final Sprite SMALL_HOVER = new Sprite(444, 100, 42, 18);

    static final Sprite PERM_OFF = new Sprite(444, 0, 18, 18);
    static final Sprite PERM_OFF_HOVER = new Sprite(444, 20, 18, 18);
    static final Sprite PERM_ON = new Sprite(444, 40, 18, 18);
    static final Sprite PERM_ON_HOVER = new Sprite(444, 60, 18, 18);

    static final Sprite SCROLL_TRACK = new Sprite(486, 124, 6, 100);
    static final Sprite SCROLL_THUMB = new Sprite(494, 124, 6, 40);

    // ==================== 布局 ====================

    static final int MARGIN = 8;
    static final int TAB_Y = 26;
    static final int TAB_H = 18;
    static final int TAB_GAP = 4;
    /** 窄页签宽度:4 页签并排时总宽 4*54+3*4=228,在 240 面板内居中. */
    static final int TAB_SM_W = 54;
    /** 标签页下方内容区起始 y. */
    static final int CONTENT_Y = 54;

    // 规则页
    static final int TOGGLE_W = 110;
    static final int TOGGLE_H = 18;
    static final int TOGGLE_COL_GAP = 4;
    static final int TOGGLE_ROW_H = 22;
    static final int SLIDER_W = 224;
    static final int SLIDER_H = 22;
    static final int SLIDER_TRACK_Y = 12;
    static final int SLIDER_FILL_H = 6;
    /** 旋钮宽度,与 PdimSliderMath 的取值换算共用同一常量,保证「旋钮中心对齐鼠标」. */
    static final int SLIDER_KNOB_W = PdimSliderMath.KNOB_W;
    static final int SLIDER_KNOB_Y = 9;
    /** 规则页维度模式开关的纵向位置. */
    static final int RULES_MODE_Y = 180;

    /** 规则页两条滑条的纵向位置. */
    static final int TIME_SLIDER_Y = 128;
    static final int SPEED_SLIDER_Y = 156;

    // 权限页(行内横向坐标见 PdimPermLayout,那里保证各控件互不重叠)
    static final int PERM_ROWS_VISIBLE = 5;
    static final int ROW_Y0 = 76;
    static final int ROW_H = 20;
    static final int INVITE_LABEL_Y = 182;
    static final int INVITE_Y = 194;
    static final int INVITE_INPUT_W = 176;
    static final int INVITE_BTN_X = 190;

    // 预设页
    static final int PREVIEW_W = 100;
    static final int PREVIEW_X = 132;
    static final int PRESET_INFO_Y = 54;
    static final int PRESET_INFO_LINE_H = 11;
    static final int COLOR_HEADER_Y = 122;
    static final int COLOR_ROW_Y = 128;
    static final int COLOR_ROW_W = 118;
    static final int COLOR_ROW_H = 18;
    static final int COLOR_ROW_GAP = 4;
    static final int COLOR_SWATCH = 12;
    static final int PRESET_PREVIEW_Y = 66;
    static final int PRESET_APPLY_Y = 174;
    static final int PRESET_NOTE_Y = 196;

    // 布局页
    static final int LAYOUT_TOGGLE_Y = 54;
    static final int LAYOUT_ROAD_SLIDER_Y = 82;
    static final int LAYOUT_FLOOR_SLIDER_Y = 110;
    static final int LAYOUT_APPLY_Y = 140;
    static final int LAYOUT_NOTE_Y = 162;
    static final int LAYOUT_NOTE_LINE_H = 9;

    // 创建向导页
    static final int CREATE_OWNER_Y = 18;
    static final int CREATE_HEADER_Y = 28;
    static final int CREATE_COLOR_ROW_Y = 42;
    static final int CREATE_NOTE_Y = 112;
    static final int CREATE_BUTTON_Y = 146;

    // ==================== 文本配色 ====================

    static final int TEXT_MAIN = 0xFF413F54;
    static final int TEXT_DIM = 0xFF696D88;
    static final int TEXT_MUTED = 0xFF878FA5;
    static final int TEXT_OK = 0xFF1E7D46;
    static final int TEXT_WARN = 0xFF9A6200;
    static final int TEXT_ERROR = 0xFFB32626;

    // ==================== 绘制 ====================

    static void draw(GuiGraphics graphics, int x, int y, Sprite sprite) {
        graphics.blit(TEXTURE, x, y, sprite.u(), sprite.v(), sprite.w(), sprite.h(), TEX_SIZE, TEX_SIZE);
    }

    /**
     * 居中绘制文本,统一关闭字体阴影.
     *
     * <p>{@code GuiGraphics.drawCenteredString} 默认带字体阴影(在原字形的右下方再画一份暗色拷贝),
     * 本 GUI 的按钮底色都是浅灰,深色文字叠加深色阴影会被看成文字重影,因此所有文字都按无阴影绘制.</p>
     */
    static void drawCenteredText(GuiGraphics graphics, Font font, Component text, int centerX, int y, int color) {
        graphics.drawString(font, text, centerX - font.width(text) / 2, y, color, false);
    }

    /**
     * 取精灵左上角子区域绘制,用于滑条填充条等宽度可变的部分.
     */
    static void drawSub(GuiGraphics graphics, int x, int y, Sprite sprite, int subW, int subH) {
        graphics.blit(TEXTURE, x, y, sprite.u(), sprite.v(), subW, subH, TEX_SIZE, TEX_SIZE);
    }

    /**
     * 判断点是否落在矩形内.
     */
    static boolean isInside(double pointX, double pointY, int x, int y, int w, int h) {
        return pointX >= x && pointX < x + w && pointY >= y && pointY < y + h;
    }
}
