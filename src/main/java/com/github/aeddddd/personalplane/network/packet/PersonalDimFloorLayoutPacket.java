package com.github.aeddddd.personalplane.network.packet;

import com.github.aeddddd.personalplane.PersonalDimensionConfig;
import com.github.aeddddd.personalplane.PersonalPlaneMod;
import com.github.aeddddd.personalplane.dimension.DimensionEntry;
import com.github.aeddddd.personalplane.dimension.OwnerKey;
import com.github.aeddddd.personalplane.dimension.PersonalDimFloorLayout;
import com.github.aeddddd.personalplane.dimension.PersonalDimensionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C→S：提交地板布局修改（层数/马路/包围区块数），可选同时重铺。
 */
public class PersonalDimFloorLayoutPacket implements CustomPacketPayload {

    public static final Type<PersonalDimFloorLayoutPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PersonalPlaneMod.MOD_ID, "floor_layout"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PersonalDimFloorLayoutPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), PersonalDimFloorLayoutPacket::decode);

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

    @Override
    public Type<PersonalDimFloorLayoutPacket> type() {
        return TYPE;
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

    public static void handle(PersonalDimFloorLayoutPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)
                    || !PersonalDimensionManager.canManage(player, packet.owner)) {
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
                            "chat.personal_plane.recarpet_cooldown"), true);
                }
            }
            PacketDistributor.sendToPlayer(player,
                    PersonalDimManagerStatePacket.create(player.server, packet.pos, packet.owner, player));
        });
    }
}
