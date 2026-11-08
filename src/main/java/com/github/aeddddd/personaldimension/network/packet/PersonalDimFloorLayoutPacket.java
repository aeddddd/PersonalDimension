package com.github.aeddddd.personaldimension.network.packet;

import com.github.aeddddd.personaldimension.PersonalDimensionConfig;
import com.github.aeddddd.personaldimension.dimension.DimensionEntry;
import com.github.aeddddd.personaldimension.dimension.OwnerKey;
import com.github.aeddddd.personaldimension.dimension.PersonalDimFloorLayout;
import com.github.aeddddd.personaldimension.dimension.PersonalDimensionManager;
import com.github.aeddddd.personaldimension.network.ModNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C→S：提交地板布局修改（层数/马路/包围区块数），可选同时重铺。
 */
public class PersonalDimFloorLayoutPacket {

    private final BlockPos pos;
    private final OwnerKey owner;
    private final int floorCount;
    private final boolean roadEnabled;
    private final int roadChunks;
    private final boolean recarpet;

    public PersonalDimFloorLayoutPacket(BlockPos pos, OwnerKey owner, int floorCount, boolean roadEnabled,
            int roadChunks, boolean recarpet) {
        this.pos = pos;
        this.owner = owner;
        this.floorCount = floorCount;
        this.roadEnabled = roadEnabled;
        this.roadChunks = roadChunks;
        this.recarpet = recarpet;
    }

    public static void encode(PersonalDimFloorLayoutPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeByte(packet.owner.type().ordinal());
        buffer.writeUUID(packet.owner.id());
        buffer.writeVarInt(packet.floorCount);
        buffer.writeBoolean(packet.roadEnabled);
        buffer.writeVarInt(packet.roadChunks);
        buffer.writeBoolean(packet.recarpet);
    }

    public static PersonalDimFloorLayoutPacket decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        int type = buffer.readByte();
        OwnerKey owner = new OwnerKey(OwnerKey.Type.values()[Math.max(0,
                Math.min(type, OwnerKey.Type.values().length - 1))], buffer.readUUID());
        return new PersonalDimFloorLayoutPacket(pos, owner, buffer.readVarInt(), buffer.readBoolean(),
                buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(PersonalDimFloorLayoutPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !PersonalDimensionManager.canManage(player, packet.owner)) {
                return;
            }
            PersonalDimFloorLayout layout = new PersonalDimFloorLayout();
            layout.floorCount = packet.floorCount;
            layout.roadEnabled = packet.roadEnabled;
            layout.roadChunks = packet.roadChunks;
            DimensionEntry entry = PersonalDimensionManager.getDimensionEntry(player.server, packet.owner);
            int floorY = PersonalDimensionConfig.COMMON.personalDimensionFloorY.get();
            int previousFloors = entry.floorLayout.clampedFloorCount(floorY);
            PersonalDimensionManager.setFloorLayout(player.server, packet.owner, layout);
            if (packet.recarpet) {
                int newFloors = layout.clampedFloorCount(floorY);
                int queued = PersonalDimensionManager.recarpetFloor(player.server, packet.owner,
                        Math.max(previousFloors, newFloors));
                if (queued < 0) {
                    player.sendSystemMessage(Component.translatable(
                            "chat.personal_dimension.recarpet_cooldown"), true);
                }
            }
            ModNetwork.CHANNEL.sendTo(
                    PersonalDimManagerStatePacket.create(player.server, packet.pos, packet.owner, player),
                    player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
        });
        context.setPacketHandled(true);
    }
}
