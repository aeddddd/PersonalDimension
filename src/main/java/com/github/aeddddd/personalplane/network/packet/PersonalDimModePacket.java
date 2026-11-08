package com.github.aeddddd.personalplane.network.packet;

import com.github.aeddddd.personalplane.dimension.DimensionMode;
import com.github.aeddddd.personalplane.dimension.OwnerKey;
import com.github.aeddddd.personalplane.dimension.PersonalDimensionManager;
import com.github.aeddddd.personalplane.network.ModNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C→S：切换玩家自己的维度模式（TEAM / PERSONAL）。
 *
 * <p>只保存偏好，不自动传送；回发当前打开界面的状态，使开关立刻反映新值。
 * 新模式的生效时机是下一次使用核心物品。</p>
 */
public class PersonalDimModePacket {

    private final BlockPos pos;
    private final OwnerKey owner;
    private final DimensionMode mode;

    public PersonalDimModePacket(BlockPos pos, OwnerKey owner, DimensionMode mode) {
        this.pos = pos;
        this.owner = owner;
        this.mode = mode;
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

    public static void handle(PersonalDimModePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            PersonalDimensionManager.setMode(player.server, player.getUUID(), packet.mode);
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    packet.mode == DimensionMode.TEAM
                            ? "chat.personal_plane.mode_team"
                            : "chat.personal_plane.mode_personal"), true);
            ModNetwork.CHANNEL.sendTo(
                    PersonalDimManagerStatePacket.create(player.server, packet.pos, packet.owner, player),
                    player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
        });
        context.setPacketHandled(true);
    }
}
