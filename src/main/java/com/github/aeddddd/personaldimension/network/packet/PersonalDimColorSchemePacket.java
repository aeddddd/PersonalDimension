package com.github.aeddddd.personaldimension.network.packet;

import com.github.aeddddd.personaldimension.api.dimension.FloorColorScheme;
import com.github.aeddddd.personaldimension.dimension.OwnerKey;
import com.github.aeddddd.personaldimension.dimension.PersonalDimensionManager;
import com.github.aeddddd.personaldimension.network.ModNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C→S：提交地板颜色方案修改（管理器预设页），可选同时重铺已生成地板。
 */
public class PersonalDimColorSchemePacket {

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

    public static void handle(PersonalDimColorSchemePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !PersonalDimensionManager.canManage(player, packet.owner)) {
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
                            "chat.personal_dimension.recarpet_cooldown"), true);
                }
            }
            ModNetwork.CHANNEL.sendTo(
                    PersonalDimManagerStatePacket.create(player.server, packet.pos, packet.owner, player),
                    player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
        });
        context.setPacketHandled(true);
    }

    private static boolean isValidColor(int id) {
        return id >= 0 && id < 16;
    }
}
