package com.github.aeddddd.personaldimension.client.gui;

import com.github.aeddddd.personaldimension.menu.PersonalDimensionCreateMenu;
import com.github.aeddddd.personaldimension.network.ModNetwork;
import com.github.aeddddd.personaldimension.network.packet.PersonalDimCreateSubmitPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;

/**
 * 个人维度创建向导：先选地板配色，确认后创建当前模式对应的玩家/队伍维度并进入。
 */
public class PersonalDimensionCreateScreen extends PdimScreen<PersonalDimensionCreateMenu> {

    public PersonalDimensionCreateScreen(PersonalDimensionCreateMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = PdimStyle.PANEL_W;
        this.imageHeight = PdimStyle.CREATE_PANEL_H;
    }

    @Override
    protected void init() {
        super.init();
        PdimColorEditor editor = new PdimColorEditor(leftPos + PdimStyle.MARGIN,
                topPos + PdimStyle.CREATE_COLOR_ROW_Y, leftPos, topPos, PdimStyle.PANEL_W,
                PdimStyle.CREATE_PANEL_H);
        colorEditor = editor;

        addRenderableWidget(PdimButton.action(leftPos + PdimStyle.MARGIN,
                topPos + PdimStyle.CREATE_BUTTON_Y, Component.translatable("gui.personal_dimension.pdim.create"),
                Component.translatable("gui.personal_dimension.pdim.create.tip"), button -> {
                    DyeColor road = editor.road();
                    DyeColor line = editor.line();
                    DyeColor platform = editor.platform();
                    ModNetwork.CHANNEL.sendToServer(new PersonalDimCreateSubmitPacket(menu.owner,
                            road.getId(), line.getId(), platform.getId()));
                }));
        addRenderableWidget(PdimButton.action(leftPos + PdimStyle.PREVIEW_X,
                topPos + PdimStyle.CREATE_BUTTON_Y, Component.translatable("gui.personal_dimension.pdim.cancel"),
                null, button -> onClose()));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (zoomPreviewOnScroll(mouseX, mouseY, delta, PdimStyle.CREATE_COLOR_ROW_Y)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        PdimStyle.draw(graphics, leftPos, topPos, PdimStyle.PANEL_CREATE);
        Component title = Component.translatable("gui.personal_dimension.pdim.create_title");
        graphics.drawString(font, title, leftPos + (PdimStyle.PANEL_W - font.width(title)) / 2, topPos + 6,
                PdimStyle.TEXT_MAIN, false);

        Component ownerLine = Component.translatable(menu.owner.isTeam()
                        ? "gui.personal_dimension.pdim.create.team"
                        : "gui.personal_dimension.pdim.create.personal",
                menu.ownerName);
        int ownerY = topPos + PdimStyle.CREATE_OWNER_Y;
        graphics.enableScissor(leftPos + PdimStyle.MARGIN, ownerY,
                leftPos + PdimStyle.PANEL_W - PdimStyle.MARGIN, ownerY + 9);
        graphics.drawString(font, ownerLine, leftPos + PdimStyle.MARGIN, ownerY,
                menu.owner.isTeam() ? PdimStyle.TEXT_OK : PdimStyle.TEXT_DIM, false);
        graphics.disableScissor();

        int left = leftPos + PdimStyle.MARGIN;
        graphics.drawString(font, Component.translatable("gui.personal_dimension.pdim.colors_header"), left,
                topPos + PdimStyle.CREATE_HEADER_Y, PdimStyle.TEXT_MAIN, false);
        graphics.drawString(font, Component.translatable("gui.personal_dimension.pdim.preset.preview"),
                leftPos + PdimStyle.PREVIEW_X, topPos + PdimStyle.CREATE_HEADER_Y, PdimStyle.TEXT_MAIN, false);

        int noteY = topPos + PdimStyle.CREATE_NOTE_Y;
        for (FormattedCharSequence line : font.split(
                Component.translatable("gui.personal_dimension.pdim.create.note"), PdimStyle.COLOR_ROW_W)) {
            graphics.drawString(font, line, left, noteY, PdimStyle.TEXT_MUTED, false);
            noteY += PdimStyle.PRESET_INFO_LINE_H;
        }

        DyeColor road = colorEditor != null ? colorEditor.road() : DyeColor.GRAY;
        DyeColor line = colorEditor != null ? colorEditor.line() : DyeColor.WHITE;
        DyeColor platform = colorEditor != null ? colorEditor.platform() : DyeColor.BLACK;
        PdimFloorPreview.render(graphics, leftPos + PdimStyle.PREVIEW_X, topPos + PdimStyle.CREATE_COLOR_ROW_Y,
                menu.presetWidth, menu.presetDepth, menu.presetStates, menu.presetPalette, road, line, platform,
                previewCells);
        if (colorEditor != null) {
            colorEditor.render(graphics, mouseX, mouseY);
        }
    }
}
