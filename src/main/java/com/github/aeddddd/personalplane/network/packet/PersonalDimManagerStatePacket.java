package com.github.aeddddd.personalplane.network.packet;

import com.github.aeddddd.personalplane.access.AccessPolicies;
import com.github.aeddddd.personalplane.api.dimension.FloorColorRole;
import com.github.aeddddd.personalplane.api.dimension.FloorColorScheme;
import com.github.aeddddd.personalplane.api.dimension.IFloorPreset;
import com.github.aeddddd.personalplane.client.handler.ClientPacketHandlers;
import com.github.aeddddd.personalplane.dimension.DimensionEntry;
import com.github.aeddddd.personalplane.dimension.DimensionMode;
import com.github.aeddddd.personalplane.dimension.FloorPreset;
import com.github.aeddddd.personalplane.dimension.ModFloorStyles;
import com.github.aeddddd.personalplane.dimension.OwnerKey;
import com.github.aeddddd.personalplane.dimension.OwnerResolver;
import com.github.aeddddd.personalplane.dimension.PersonalDimFloorLayout;
import com.github.aeddddd.personalplane.dimension.PersonalDimensionManager;
import com.github.aeddddd.personalplane.dimension.PersonalDimensionRules;
import com.github.aeddddd.personalplane.dimension.PresetLoader;
import com.github.aeddddd.personalplane.PersonalDimensionConfig;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * S→C：个人维度管理器的完整状态同步包（打开与变更时发送）。
 */
public class PersonalDimManagerStatePacket {

    private static final int MAX_PALETTE_SIZE = 1024;
    private static final int MAX_STATE_COUNT = 65536;

    public final BlockPos pos;
    public final OwnerKey owner;
    public final String ownerName;
    public final boolean created;
    public final PersonalDimensionRules rules;
    public final int floorY;
    public final int entryY;
    public final int presetWidth;
    public final int presetDepth;
    public final List<String> presetPalette;
    public final int[] presetStates;
    public final int roadColor;
    public final int lineColor;
    public final int platformColor;
    public final int floorCount;
    public final boolean roadEnabled;
    public final int roadChunks;
    public final boolean layoutApplies;
    public final DimensionMode mode;
    public final boolean ftbAvailable;
    public final boolean canToggleMode;
    public final int memberCount;

    private PersonalDimManagerStatePacket(BlockPos pos, OwnerKey owner, String ownerName, boolean created,
            PersonalDimensionRules rules, int floorY, int entryY, int presetWidth, int presetDepth,
            List<String> presetPalette, int[] presetStates, int roadColor, int lineColor, int platformColor,
            int floorCount, boolean roadEnabled, int roadChunks, boolean layoutApplies,
            DimensionMode mode, boolean ftbAvailable, boolean canToggleMode, int memberCount) {
        this.pos = pos;
        this.owner = owner;
        this.ownerName = ownerName;
        this.created = created;
        this.rules = rules;
        this.floorY = floorY;
        this.entryY = entryY;
        this.presetWidth = presetWidth;
        this.presetDepth = presetDepth;
        this.presetPalette = presetPalette;
        this.presetStates = presetStates;
        this.roadColor = roadColor;
        this.lineColor = lineColor;
        this.platformColor = platformColor;
        this.floorCount = floorCount;
        this.roadEnabled = roadEnabled;
        this.roadChunks = roadChunks;
        this.layoutApplies = layoutApplies;
        this.mode = mode;
        this.ftbAvailable = ftbAvailable;
        this.canToggleMode = canToggleMode;
        this.memberCount = memberCount;
    }

    public static PersonalDimManagerStatePacket create(MinecraftServer server, BlockPos pos, OwnerKey owner,
            ServerPlayer viewer) {
        DimensionEntry entry = PersonalDimensionManager.getDimensionEntry(server, owner);
        PersonalDimensionRules rules = entry.rules.copy();
        PersonalDimFloorLayout layout = entry.floorLayout;

        IFloorPreset base = PresetLoader.getPreset();
        IFloorPreset resolved = ModFloorStyles.applyLayout(base, layout);
        boolean layoutApplies = ModFloorStyles.isBuiltinStyle(base);
        FloorPreset preset = FloorPreset.from(resolved);
        List<String> palette = new ArrayList<>(preset.palette.length);
        for (net.minecraft.world.level.block.state.BlockState state : preset.palette) {
            palette.add(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
        }

        FloorColorScheme scheme = entry.colorScheme;
        int floorY = PersonalDimensionConfig.COMMON.personalDimensionFloorY.get();
        boolean canToggle = AccessPolicies.ftbAvailable()
                && AccessPolicies.teamAccess().teamOf(viewer.getUUID())
                        .map(team -> !team.personalTeam()).orElse(false);
        return new PersonalDimManagerStatePacket(pos, owner, OwnerResolver.displayName(server, owner),
                entry.created, rules, floorY,
                PersonalDimensionConfig.COMMON.personalDimensionEntryY.get(),
                preset.width, preset.depth, palette, preset.stateList,
                scheme.getConcreteColor(FloorColorRole.ROAD_BASE).getId(),
                scheme.getConcreteColor(FloorColorRole.ROAD_LINE).getId(),
                scheme.getConcreteColor(FloorColorRole.PLATFORM_BASE).getId(),
                layout.clampedFloorCount(floorY), layout.roadEnabled, layout.roadChunks,
                layoutApplies,
                PersonalDimensionManager.getMode(server, viewer.getUUID()),
                AccessPolicies.ftbAvailable(), canToggle,
                owner.isTeam() ? AccessPolicies.teamAccess().memberCount(owner.id()) : 1);
    }

    public static void encode(PersonalDimManagerStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeByte(packet.owner.type().ordinal());
        buffer.writeUUID(packet.owner.id());
        buffer.writeUtf(packet.ownerName);
        buffer.writeBoolean(packet.created);
        PersonalDimensionRules rules = packet.rules;
        buffer.writeBoolean(rules.disableMobSpawning);
        buffer.writeBoolean(rules.lockWeather);
        buffer.writeBoolean(rules.lockTime);
        buffer.writeBoolean(rules.daylightCycle);
        buffer.writeLong(rules.timeValue);
        buffer.writeBoolean(rules.flightEnabled);
        buffer.writeFloat(rules.movementSpeed);
        buffer.writeBoolean(rules.noFlightInertia);

        buffer.writeVarInt(packet.floorY);
        buffer.writeVarInt(packet.entryY);
        buffer.writeVarInt(packet.presetWidth);
        buffer.writeVarInt(packet.presetDepth);
        buffer.writeVarInt(packet.presetPalette.size());
        for (String name : packet.presetPalette) {
            buffer.writeUtf(name);
        }
        buffer.writeVarInt(packet.presetStates.length);
        for (int state : packet.presetStates) {
            buffer.writeVarInt(state);
        }
        buffer.writeVarInt(packet.roadColor);
        buffer.writeVarInt(packet.lineColor);
        buffer.writeVarInt(packet.platformColor);
        buffer.writeVarInt(packet.floorCount);
        buffer.writeBoolean(packet.roadEnabled);
        buffer.writeVarInt(packet.roadChunks);
        buffer.writeBoolean(packet.layoutApplies);
        buffer.writeByte(packet.mode.ordinal());
        buffer.writeBoolean(packet.ftbAvailable);
        buffer.writeBoolean(packet.canToggleMode);
        buffer.writeVarInt(packet.memberCount);
    }

    public static PersonalDimManagerStatePacket decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        int ownerType = buffer.readByte();
        OwnerKey owner = new OwnerKey(OwnerKey.Type.values()[Math.max(0,
                Math.min(ownerType, OwnerKey.Type.values().length - 1))], buffer.readUUID());
        String ownerName = buffer.readUtf();
        boolean created = buffer.readBoolean();
        PersonalDimensionRules rules = new PersonalDimensionRules();
        rules.disableMobSpawning = buffer.readBoolean();
        rules.lockWeather = buffer.readBoolean();
        rules.lockTime = buffer.readBoolean();
        rules.daylightCycle = buffer.readBoolean();
        rules.timeValue = buffer.readLong();
        rules.flightEnabled = buffer.readBoolean();
        rules.movementSpeed = buffer.readFloat();
        rules.noFlightInertia = buffer.readBoolean();

        int floorY = buffer.readVarInt();
        int entryY = buffer.readVarInt();
        int width = buffer.readVarInt();
        int depth = buffer.readVarInt();
        int paletteSize = readClampedCount(buffer, "presetPalette", MAX_PALETTE_SIZE);
        List<String> palette = new ArrayList<>(paletteSize);
        for (int i = 0; i < paletteSize; i++) {
            palette.add(buffer.readUtf());
        }
        int stateCount = readClampedCount(buffer, "presetStates", MAX_STATE_COUNT);
        int[] states = new int[stateCount];
        for (int i = 0; i < stateCount; i++) {
            states[i] = buffer.readVarInt();
        }
        int roadColor = buffer.readVarInt();
        int lineColor = buffer.readVarInt();
        int platformColor = buffer.readVarInt();
        int floorCount = buffer.readVarInt();
        boolean roadEnabled = buffer.readBoolean();
        int roadChunks = buffer.readVarInt();
        boolean layoutApplies = buffer.readBoolean();
        int modeOrdinal = buffer.readByte();
        DimensionMode mode = DimensionMode.values()[Math.max(0,
                Math.min(modeOrdinal, DimensionMode.values().length - 1))];
        boolean ftbAvailable = buffer.readBoolean();
        boolean canToggleMode = buffer.readBoolean();
        int memberCount = buffer.readVarInt();

        return new PersonalDimManagerStatePacket(pos, owner, ownerName, created, rules, floorY, entryY,
                width, depth, palette, states, roadColor, lineColor, platformColor, floorCount,
                roadEnabled, roadChunks, layoutApplies, mode, ftbAvailable, canToggleMode, memberCount);
    }

    private static int readClampedCount(FriendlyByteBuf buffer, String field, int max) {
        int count = buffer.readVarInt();
        if (count < 0 || count > max) {
            throw new DecoderException("personal dimension state field " + field + " out of range: " + count);
        }
        return count;
    }

    public static void handle(PersonalDimManagerStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientPacketHandlers.updatePersonalDimState(packet);
            }
        });
        context.setPacketHandled(true);
    }
}
