package com.github.aeddddd.personaldimension.menu;

import com.github.aeddddd.personaldimension.dimension.FloorPreset;
import com.github.aeddddd.personaldimension.dimension.OwnerKey;
import com.github.aeddddd.personaldimension.dimension.OwnerResolver;
import com.github.aeddddd.personaldimension.dimension.PresetLoader;
import com.github.aeddddd.personaldimension.registry.ModMenus;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * 个人维度创建向导菜单：无槽位，打开时一次性下发预设预览与归属信息。
 */
public class PersonalDimensionCreateMenu extends AbstractContainerMenu {

    public final OwnerKey owner;
    public final String ownerName;
    public final int presetWidth;
    public final int presetDepth;
    public final List<String> presetPalette;
    public final int[] presetStates;

    public PersonalDimensionCreateMenu(int id, Inventory inv, OwnerKey owner, String ownerName,
            int presetWidth, int presetDepth, List<String> presetPalette, int[] presetStates) {
        super(ModMenus.PERSONAL_DIMENSION_CREATE.get(), id);
        this.owner = owner;
        this.ownerName = ownerName;
        this.presetWidth = presetWidth;
        this.presetDepth = presetDepth;
        this.presetPalette = presetPalette;
        this.presetStates = presetStates;
    }

    public PersonalDimensionCreateMenu(int id, Inventory inv, OwnerKey owner) {
        this(id, inv, owner, "", 0, 0, List.of(), new int[0]);
    }

    public static PersonalDimensionCreateMenu create(int id, Inventory inv, FriendlyByteBuf buf) {
        int type = buf.readByte();
        OwnerKey owner = new OwnerKey(OwnerKey.Type.values()[Math.max(0,
                Math.min(type, OwnerKey.Type.values().length - 1))], buf.readUUID());
        String ownerName = buf.readUtf();
        int width = buf.readVarInt();
        int depth = buf.readVarInt();
        int paletteSize = buf.readVarInt();
        List<String> palette = new ArrayList<>(paletteSize);
        for (int i = 0; i < paletteSize; i++) {
            palette.add(buf.readUtf());
        }
        int stateCount = buf.readVarInt();
        int[] states = new int[stateCount];
        for (int i = 0; i < stateCount; i++) {
            states[i] = buf.readVarInt();
        }
        return new PersonalDimensionCreateMenu(id, inv, owner, ownerName, width, depth, palette, states);
    }

    /**
     * 写入打开界面时的归属与地板样式预览数据（服务端调用）。
     */
    public static void writePreviewData(FriendlyByteBuf buf, OwnerKey owner, String ownerName) {
        FloorPreset preset = FloorPreset.from(PresetLoader.getPreset());
        buf.writeByte(owner.type().ordinal());
        buf.writeUUID(owner.id());
        buf.writeUtf(ownerName);
        buf.writeVarInt(preset.width);
        buf.writeVarInt(preset.depth);
        buf.writeVarInt(preset.palette.length);
        for (BlockState state : preset.palette) {
            buf.writeUtf(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
        }
        buf.writeVarInt(preset.stateList.length);
        for (int state : preset.stateList) {
            buf.writeVarInt(state);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
