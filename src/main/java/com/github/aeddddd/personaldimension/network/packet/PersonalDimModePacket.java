package com.github.aeddddd.personaldimension.network.packet;

import com.github.aeddddd.personaldimension.PersonalDimensionMod;
import com.github.aeddddd.personaldimension.dimension.DimensionMode;
import com.github.aeddddd.personaldimension.dimension.OwnerKey;
import com.github.aeddddd.personaldimension.dimension.PersonalDimensionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C→S：切换玩家自己的维度模式（TEAM / PERSONAL）。只保存偏好，不自动传送。
 */
public class PersonalDimModePacket implements CustomPacketPayload {

    public static final Type<PersonalDimModePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PersonalDimensionMod.MOD_ID, "mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PersonalDimModePacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), PersonalDimModePacket::decode);

    private final BlockPos pos;
    private final OwnerKey owner;
    private final DimensionMode mode;

    public PersonalDimModePacket(BlockPos pos, OwnerKey owner, DimensionMode mode) {
        this.pos = pos;
        this.owner = owner;
        this.mode = mode;
    }

    @Override
    public Type<PersonalDimModePacket> type() {
        return TYPE;
    }

    public static void encode(PersonalDimModePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeByte(packet.owner.type().ordinal());
        buffer.writeUUID(packet.owner.id());
        buffer.writeByte(packet.mode.ordinal());
    }

    public static PersonalDimModePacket decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        int ownerType = buffer.readByte();
        OwnerKey owner = new OwnerKey(OwnerKey.Type.values()[Math.max(0,
                Math.min(ownerType, OwnerKey.Type.values().length - 1))], buffer.readUUID());
        int mode = buffer.readByte();
        return new PersonalDimModePacket(pos, owner,
                DimensionMode.values()[Math.max(0, Math.min(mode, DimensionMode.values().length - 1))]);
    }

    public static void handle(PersonalDimModePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            PersonalDimensionManager.setMode(player.server, player.getUUID(), packet.mode);
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    packet.mode == DimensionMode.TEAM
                            ? "chat.personal_dimension.mode_team"
                            : "chat.personal_dimension.mode_personal"), true);
            PacketDistributor.sendToPlayer(player,
                    PersonalDimManagerStatePacket.create(player.server, packet.pos, packet.owner, player));
        });
    }
}
