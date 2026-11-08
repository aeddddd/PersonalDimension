package com.github.aeddddd.personalplane.network.packet;

import com.github.aeddddd.personalplane.PersonalPlaneMod;
import com.github.aeddddd.personalplane.api.dimension.FloorColorScheme;
import com.github.aeddddd.personalplane.dimension.OwnerKey;
import com.github.aeddddd.personalplane.dimension.PersonalDimensionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C→S：提交地板颜色方案修改（管理器预设页），可选同时重铺已生成地板。
 */
public class PersonalDimColorSchemePacket implements CustomPacketPayload {

    public static final Type<PersonalDimColorSchemePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PersonalPlaneMod.MOD_ID, "color_scheme"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PersonalDimColorSchemePacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), PersonalDimColorSchemePacket::decode);

    private final BlockPos pos;
    private final OwnerKey owner;
    private final int roadColor;
    private final int lineColor;
    private final int platformColor;
    private final boolean recarpet;

    public PersonalDimColorSchemePacket(BlockPos pos, OwnerKey owner, int roadColor, int lineColor,
            int platformColor, boolean recarpet) {
        this.pos = pos;
        this.owner = owner;
        this.roadColor = roadColor;
        this.lineColor = lineColor;
        this.platformColor = platformColor;
        this.recarpet = recarpet;
    }

    @Override
    public Type<PersonalDimColorSchemePacket> type() {
        return TYPE;
    }

    public static void encode(PersonalDimColorSchemePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeByte(packet.owner.type().ordinal());
        buffer.writeUUID(packet.owner.id());
        buffer.writeVarInt(packet.roadColor);
        buffer.writeVarInt(packet.lineColor);
        buffer.writeVarInt(packet.platformColor);
        buffer.writeBoolean(packet.recarpet);
    }

    public static PersonalDimColorSchemePacket decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        int type = buffer.readByte();
        OwnerKey owner = new OwnerKey(OwnerKey.Type.values()[Math.max(0,
                Math.min(type, OwnerKey.Type.values().length - 1))], buffer.readUUID());
        return new PersonalDimColorSchemePacket(pos, owner, buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(PersonalDimColorSchemePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)
                    || !PersonalDimensionManager.canManage(player, packet.owner)) {
                return;
            }
            if (!isValidColor(packet.roadColor) || !isValidColor(packet.lineColor)
                    || !isValidColor(packet.platformColor)) {
                return;
            }
            FloorColorScheme scheme = FloorColorScheme.ofConcrete(
                    DyeColor.byId(packet.roadColor), DyeColor.byId(packet.lineColor),
                    DyeColor.byId(packet.platformColor));
            PersonalDimensionManager.setColorScheme(player.server, packet.owner, scheme);
            if (packet.recarpet) {
                int queued = PersonalDimensionManager.recarpetFloor(player.server, packet.owner);
                if (queued < 0) {
                    player.sendSystemMessage(Component.translatable(
                            "chat.personal_plane.recarpet_cooldown"), true);
                }
            }
            PacketDistributor.sendToPlayer(player,
                    PersonalDimManagerStatePacket.create(player.server, packet.pos, packet.owner, player));
        });
    }

    private static boolean isValidColor(int id) {
        return id >= 0 && id < 16;
    }
}
