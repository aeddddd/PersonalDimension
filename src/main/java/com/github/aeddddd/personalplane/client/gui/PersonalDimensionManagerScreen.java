package com.github.aeddddd.personalplane.client.gui;

import com.github.aeddddd.personalplane.dimension.DimensionMode;
import com.github.aeddddd.personalplane.dimension.PersonalDimFloorLayout;
import com.github.aeddddd.personalplane.dimension.PersonalDimensionRules;
import com.github.aeddddd.personalplane.menu.PersonalDimensionManagerMenu;
import com.github.aeddddd.personalplane.network.ModNetwork;
import com.github.aeddddd.personalplane.network.packet.PersonalDimColorSchemePacket;
import com.github.aeddddd.personalplane.network.packet.PersonalDimFloorLayoutPacket;
import com.github.aeddddd.personalplane.network.packet.PersonalDimManagerStatePacket;
import com.github.aeddddd.personalplane.network.packet.PersonalDimModePacket;
import com.github.aeddddd.personalplane.network.packet.PersonalDimRulesSubmitPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;

import javax.annotation.Nullable;
import java.util.Locale;

/**
 * 个人维度管理器界面：规则、预设、布局三个标签页。
 *
 * <p>访问控制由 FTB Teams 承担，原「权限」页签已移除；规则页底部提供玩家级
 * 「队伍/个人」维度模式开关。</p>
 */
public class PersonalDimensionManagerScreen extends PdimScreen<PersonalDimensionManagerMenu> {

    private static final int TAB_RULES = 0;
    private static final int TAB_PRESET = 1;
    private static final int TAB_LAYOUT = 2;

    private static final String[] TAB_KEYS = {
            "gui.personal_plane.pdim.tab.rules",
            "gui.personal_plane.pdim.tab.preset",
            "gui.personal_plane.pdim.tab.layout"
    };

    @Nullable
    private PersonalDimManagerStatePacket state;
    private int tab = TAB_RULES;
    @Nullable
    private PdimSlider timeSlider;
    @Nullable
    private PdimSlider speedSlider;
    @Nullable
    private DimensionMode pendingMode;
    private int layoutFloors = -1;
    private boolean layoutRoad = true;
    private int layoutRoadChunks = PersonalDimFloorLayout.DEFAULT_ROAD_CHUNKS;

    public PersonalDimensionManagerScreen(PersonalDimensionManagerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = PdimStyle.PANEL_W;
        this.imageHeight = PdimStyle.PANEL_H;
    }

    private static Component formatTime(double ticks) {
        int value = (int) Math.round(ticks);
        int hours = (value / 1000 + 6) % 24;
        int minutes = (value % 1000) * 60 / 1000;
        return Component.literal(String.format(Locale.ROOT, "%02d:%02d", hours, minutes));
    }

    @Override
    protected void init() {
        super.init();
        rebuildWidgets();
    }

    private static Component stateText(boolean on) {
        return Component.translatable(on ? "gui.personal_plane.pdim.on" : "gui.personal_plane.pdim.off");
    }

    private void switchTab(int newTab) {
        tab = newTab;
        rebuildWidgets();
    }

    public void updateState(PersonalDimManagerStatePacket packet) {
        if (!packet.pos.equals(this.menu.pos)) {
            return;
        }
        this.state = packet;
        this.pendingMode = packet.mode;
        this.layoutFloors = packet.floorCount;
        this.layoutRoad = packet.roadEnabled;
        this.layoutRoadChunks = packet.roadChunks;
        rebuildWidgets();
    }

    @Override
    protected void rebuildWidgets() {
        clearWidgets();
        timeSlider = null;
        speedSlider = null;
        colorEditor = null;

        int tabsWidth = TAB_KEYS.length * PdimStyle.TAB_SM_W + (TAB_KEYS.length - 1) * PdimStyle.TAB_GAP;
        int tabX0 = leftPos + (PdimStyle.PANEL_W - tabsWidth) / 2;
        for (int i = 0; i < TAB_KEYS.length; i++) {
            int index = i;
            addRenderableWidget(PdimButton.tab(tabX0 + i * (PdimStyle.TAB_SM_W + PdimStyle.TAB_GAP),
                    topPos + PdimStyle.TAB_Y, tab == index, Component.translatable(TAB_KEYS[i]),
                    button -> switchTab(index)));
        }

        if (state == null) {
            return;
        }
        switch (tab) {
            case TAB_PRESET -> initPresetTab();
            case TAB_LAYOUT -> initLayoutTab();
            default -> initRulesTab();
        }
    }

    private void initRulesTab() {
        PersonalDimensionRules rules = state.rules;
        int left = leftPos + PdimStyle.MARGIN;
        int right = left + PdimStyle.TOGGLE_W + PdimStyle.TOGGLE_COL_GAP;
        int row0 = topPos + PdimStyle.CONTENT_Y;
        int step = PdimStyle.TOGGLE_ROW_H;

        addToggle(left, row0, "gui.personal_plane.pdim.disable_mob_spawning", rules.disableMobSpawning,
                value -> rules.disableMobSpawning = value);
        addToggle(right, row0, "gui.personal_plane.pdim.lock_weather", rules.lockWeather,
                value -> rules.lockWeather = value);
        addToggle(left, row0 + step, "gui.personal_plane.pdim.lock_time", rules.lockTime,
                value -> rules.lockTime = value);
        addToggle(right, row0 + step, "gui.personal_plane.pdim.daylight_cycle", rules.daylightCycle,
                value -> rules.daylightCycle = value);
        addToggle(left, row0 + step * 2, "gui.personal_plane.pdim.flight", rules.flightEnabled,
                value -> rules.flightEnabled = value);
        addToggle(right, row0 + step * 2, "gui.personal_plane.pdim.no_inertia", rules.noFlightInertia,
                value -> rules.noFlightInertia = value);

        timeSlider = new PdimSlider(left, topPos + PdimStyle.TIME_SLIDER_Y, PdimStyle.SLIDER_W, 0, 23999, 1,
                rules.timeValue, Component.translatable("gui.personal_plane.pdim.time_value"),
                PersonalDimensionManagerScreen::formatTime,
                value -> rules.timeValue = (long) value, this::submitRules,
                Component.translatable("gui.personal_plane.pdim.time_value.desc"));
        addRenderableWidget(timeSlider);

        speedSlider = new PdimSlider(left, topPos + PdimStyle.SPEED_SLIDER_Y, PdimStyle.SLIDER_W, 5, 200, 1,
                rules.movementSpeed * 100.0, Component.translatable("gui.personal_plane.pdim.movement_speed"),
                value -> Component.literal(Math.round(value) + "%"),
                value -> rules.movementSpeed = (float) (value / 100.0), this::submitRules,
                Component.translatable("gui.personal_plane.pdim.movement_speed.desc"));
        addRenderableWidget(speedSlider);

        // 玩家级维度模式开关：TEAM = 跟随 FTB 队伍，PERSONAL = 始终使用个人维度
        Component modeLabel = Component.translatable("gui.personal_plane.pdim.mode",
                Component.translatable(pendingMode == DimensionMode.TEAM
                        ? "gui.personal_plane.pdim.mode.team"
                        : "gui.personal_plane.pdim.mode.personal"));
        Component modeHint = Component.translatable("gui.personal_plane.pdim.mode.tip",
                Component.translatable(pendingMode == DimensionMode.TEAM
                        ? "gui.personal_plane.pdim.mode.team"
                        : "gui.personal_plane.pdim.mode.personal"));
        PdimButton modeButton = PdimButton.toggle(left, topPos + PdimStyle.RULES_MODE_Y,
                pendingMode == DimensionMode.TEAM, modeLabel, modeHint, button -> {
                    DimensionMode next = pendingMode == DimensionMode.TEAM
                            ? DimensionMode.PERSONAL
                            : DimensionMode.TEAM;
                    pendingMode = next;
                    rebuildWidgets();
                    ModNetwork.CHANNEL.sendToServer(new PersonalDimModePacket(menu.pos, menu.owner, next));
                });
        modeButton.active = state.canToggleMode;
        addRenderableWidget(modeButton);
    }

    private void addToggle(int x, int y, String labelKey, boolean value, java.util.function.Consumer<Boolean> setter) {
        Component label = Component.translatable(labelKey);
        Component hint = Component.translatable(labelKey).append(": ").append(stateText(value))
                .append("\n").append(Component.translatable(labelKey + ".desc"));
        addRenderableWidget(PdimButton.toggle(x, y, value, label, hint, button -> {
            setter.accept(!value);
            submitRules();
            rebuildWidgets();
        }));
    }

    private void submitRules() {
        ModNetwork.CHANNEL.sendToServer(
                new PersonalDimRulesSubmitPacket(menu.pos, menu.owner, state.rules.copy()));
    }

    private void initPresetTab() {
        PdimColorEditor editor = new PdimColorEditor(leftPos + PdimStyle.MARGIN,
                topPos + PdimStyle.COLOR_ROW_Y, leftPos, topPos, PdimStyle.PANEL_W, PdimStyle.PANEL_H);
        editor.setColors(state.roadColor, state.lineColor, state.platformColor);
        colorEditor = editor;
        addRenderableWidget(PdimButton.action(leftPos + PdimStyle.PREVIEW_X, topPos + PdimStyle.PRESET_APPLY_Y,
                Component.translatable("gui.personal_plane.pdim.apply_recolor"),
                Component.translatable("gui.personal_plane.pdim.apply_recolor.tip"), button -> {
                    DyeColor road = editor.road();
                    DyeColor line = editor.line();
                    DyeColor platform = editor.platform();
                    ModNetwork.CHANNEL.sendToServer(new PersonalDimColorSchemePacket(menu.pos, menu.owner,
                            road.getId(), line.getId(), platform.getId(), true));
                }));
    }

    private void initLayoutTab() {
        int left = leftPos + PdimStyle.MARGIN;

        Component roadLabel = Component.translatable("gui.personal_plane.pdim.road_enabled");
        Component roadHint = roadLabel.copy().append(": ").append(stateText(layoutRoad))
                .append("\n").append(Component.translatable("gui.personal_plane.pdim.road_enabled.desc"));
        addRenderableWidget(PdimButton.toggle(left, topPos + PdimStyle.LAYOUT_TOGGLE_Y, layoutRoad, roadLabel,
                roadHint, button -> {
                    layoutRoad = !layoutRoad;
                    rebuildWidgets();
                }));

        PdimSlider roadSlider = new PdimSlider(left, topPos + PdimStyle.LAYOUT_ROAD_SLIDER_Y, PdimStyle.SLIDER_W,
                PersonalDimFloorLayout.MIN_ROAD_CHUNKS, PersonalDimFloorLayout.MAX_ROAD_CHUNKS, 1, layoutRoadChunks,
                Component.translatable("gui.personal_plane.pdim.road_chunks"),
                value -> Component.literal((int) value + " x " + (int) value),
                value -> layoutRoadChunks = (int) value, () -> {
                }, Component.translatable("gui.personal_plane.pdim.road_chunks.desc"));
        roadSlider.active = layoutRoad;
        addRenderableWidget(roadSlider);

        int maxFloors = PersonalDimFloorLayout.maxFloorCount(state.floorY);
        addRenderableWidget(new PdimSlider(left, topPos + PdimStyle.LAYOUT_FLOOR_SLIDER_Y, PdimStyle.SLIDER_W,
                1, maxFloors, 1, layoutFloors,
                Component.translatable("gui.personal_plane.pdim.floor_count"),
                value -> Component.literal(String.valueOf((int) value)),
                value -> layoutFloors = (int) value, () -> {
                }, Component.translatable("gui.personal_plane.pdim.floor_count.desc",
                        PersonalDimFloorLayout.LAYER_SPACING)));

        addRenderableWidget(PdimButton.action(left, topPos + PdimStyle.LAYOUT_APPLY_Y,
                Component.translatable("gui.personal_plane.pdim.apply_layout"),
                Component.translatable("gui.personal_plane.pdim.apply_layout.tip"), button -> submitLayout()));
    }

    private void submitLayout() {
        ModNetwork.CHANNEL.sendToServer(new PersonalDimFloorLayoutPacket(menu.pos, menu.owner,
                layoutFloors, layoutRoad, layoutRoadChunks, true));
    }

    private void renderLayoutTab(GuiGraphics graphics) {
        int left = leftPos + PdimStyle.MARGIN;
        int width = PdimStyle.PANEL_W - PdimStyle.MARGIN * 2;
        int y = topPos + PdimStyle.LAYOUT_NOTE_Y;
        y = drawWrapped(graphics, Component.translatable("gui.personal_plane.pdim.layout_note"),
                left, y, width, PdimStyle.TEXT_MUTED);
        y = drawWrapped(graphics, Component.translatable("gui.personal_plane.pdim.layout_note2",
                PersonalDimFloorLayout.LAYER_SPACING), left, y, width, PdimStyle.TEXT_MUTED);
        if (!state.layoutApplies) {
            drawWrapped(graphics, Component.translatable("gui.personal_plane.pdim.layout_custom_preset"),
                    left, y, width, PdimStyle.TEXT_WARN);
        }
    }

    private int drawWrapped(GuiGraphics graphics, Component text, int x, int y, int width, int color) {
        for (FormattedCharSequence line : font.split(text, width)) {
            graphics.drawString(font, line, x, y, color, false);
            y += PdimStyle.LAYOUT_NOTE_LINE_H;
        }
        return y;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && getFocused() instanceof PdimSlider slider && slider.isHandleDragging()) {
            return slider.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseReleased(mouseX, mouseY, button);
        if (button == 0 && getFocused() instanceof PdimSlider slider) {
            slider.endDrag();
        }
        return handled;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta != 0.0 && state != null && tab == TAB_PRESET
                && zoomPreviewOnScroll(mouseX, mouseY, delta, PdimStyle.PRESET_PREVIEW_Y)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        PdimStyle.draw(graphics, leftPos, topPos, PdimStyle.PANEL_MANAGER);
        Component title = Component.translatable("gui.personal_plane.personal_plane_manager");
        graphics.drawString(font, title, leftPos + (PdimStyle.PANEL_W - font.width(title)) / 2, topPos + 6,
                PdimStyle.TEXT_MAIN, false);

        if (state == null) {
            Component loading = Component.translatable("gui.personal_plane.pdim.loading");
            graphics.drawString(font, loading, leftPos + (PdimStyle.PANEL_W - font.width(loading)) / 2,
                    topPos + PdimStyle.PANEL_H / 2, PdimStyle.TEXT_MUTED, false);
            return;
        }

        switch (tab) {
            case TAB_PRESET -> renderPresetTab(graphics, mouseX, mouseY);
            case TAB_LAYOUT -> renderLayoutTab(graphics);
            default -> renderRulesTab(graphics);
        }
    }

    private void renderRulesTab(GuiGraphics graphics) {
        int left = leftPos + PdimStyle.MARGIN;
        graphics.drawString(font, Component.translatable("gui.personal_plane.pdim.rules_hint"), left,
                topPos + 190, PdimStyle.TEXT_DIM, false);
        if (!state.rules.lockTime) {
            graphics.drawString(font, Component.translatable("gui.personal_plane.pdim.time_locked_hint"), left,
                    topPos + 202, PdimStyle.TEXT_WARN, false);
        }
    }

    private void renderPresetTab(GuiGraphics graphics, int mouseX, int mouseY) {
        int left = leftPos + PdimStyle.MARGIN;
        int y = topPos + PdimStyle.PRESET_INFO_Y;
        int lineH = PdimStyle.PRESET_INFO_LINE_H;

        // 长队伍名会压到右侧 "Floor preview" 标签,按左栏宽度裁切
        int ownerWidth = PdimStyle.PREVIEW_X - PdimStyle.MARGIN - 4;
        graphics.enableScissor(left, y, left + ownerWidth, y + lineH);
        graphics.drawString(font, Component.translatable("gui.personal_plane.pdim.owner", state.ownerName),
                left, y, PdimStyle.TEXT_MAIN, false);
        graphics.disableScissor();
        if (state.owner.isTeam()) {
            y += lineH;
            graphics.drawString(font, Component.translatable("gui.personal_plane.pdim.members",
                    state.memberCount), left, y, PdimStyle.TEXT_DIM, false);
        }
        y += lineH;
        boolean created = state.created;
        graphics.drawString(font, Component.translatable(created
                ? "gui.personal_plane.pdim.created" : "gui.personal_plane.pdim.not_created"), left, y,
                created ? PdimStyle.TEXT_OK : PdimStyle.TEXT_ERROR, false);
        y += lineH;
        graphics.drawString(font, Component.translatable("gui.personal_plane.pdim.floor_y", state.floorY),
                left, y, PdimStyle.TEXT_DIM, false);
        y += lineH;
        graphics.drawString(font, Component.translatable("gui.personal_plane.pdim.entry_y", state.entryY),
                left, y, PdimStyle.TEXT_DIM, false);
        y += lineH;
        graphics.drawString(font, Component.translatable("gui.personal_plane.pdim.preset_size",
                state.presetWidth, state.presetDepth), left, y, PdimStyle.TEXT_DIM, false);

        graphics.drawString(font, Component.translatable("gui.personal_plane.pdim.colors_header"), left,
                topPos + PdimStyle.COLOR_HEADER_Y, PdimStyle.TEXT_MAIN, false);
        graphics.drawString(font, Component.translatable(created
                        ? "gui.personal_plane.pdim.preset.applied" : "gui.personal_plane.pdim.preset_note"),
                left, topPos + PdimStyle.PRESET_NOTE_Y, PdimStyle.TEXT_MUTED, false);

        graphics.drawString(font, Component.translatable("gui.personal_plane.pdim.preset.preview"),
                leftPos + PdimStyle.PREVIEW_X, topPos + PdimStyle.PRESET_INFO_Y, PdimStyle.TEXT_MAIN, false);
        DyeColor road = colorEditor != null ? colorEditor.road() : DyeColor.GRAY;
        DyeColor line = colorEditor != null ? colorEditor.line() : DyeColor.WHITE;
        DyeColor platform = colorEditor != null ? colorEditor.platform() : DyeColor.BLACK;
        PdimFloorPreview.render(graphics, leftPos + PdimStyle.PREVIEW_X, topPos + PdimStyle.PRESET_PREVIEW_Y,
                state.presetWidth, state.presetDepth, state.presetStates, state.presetPalette, road, line, platform,
                previewCells);
        if (colorEditor != null) {
            colorEditor.render(graphics, mouseX, mouseY);
        }
    }
}
