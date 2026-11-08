package com.github.aeddddd.personaldimension.network.packet;

import com.github.aeddddd.personaldimension.api.dimension.FloorColorScheme;
import com.github.aeddddd.personaldimension.dimension.DimensionEntry;
import com.github.aeddddd.personaldimension.dimension.OwnerKey;
import com.github.aeddddd.personaldimension.dimension.PersonalDimensionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C→S：创建向导确认提交，按所选颜色方案创建当前模式对应的维度并进入。
 *
 * <p>服务端会重新解析归属并与包内 owner 比对，防止玩家在向导打开期间切换模式导致建错维度。
 * 队伍维度可能已被队友创建：此时忽略颜色并直接进入。</p>
 */
public class PersonalDimCreateSubmitPacket {

    private final OwnerKey owner;
    private final int roadColor;
    private final int lineColor;
    private final int platformColor;

    public PersonalDimCreateSubmitPacket(OwnerKey owner, int roadColor, int lineColor, int platformColor) {
        this.owner = owner;
        this.roadColor = roadColor;
        this.lineColor = lineColor;
        this.platformColor = platformColor;
    }

    public static void encode(PersonalDimCreateSubmitPacket packet, FriendlyByteBuf buffer) {
        buffer.writeByte(packet.owner.type().ordinal());
        buffer.writeUUID(packet.owner.id());
        buffer.writeVarInt(packet.roadColor);
        buffer.writeVarInt(packet.lineColor);
        buffer.writeVarInt(packet.platformColor);
    }

    public static PersonalDimCreateSubmitPacket decode(FriendlyByteBuf buffer) {
        int type = buffer.readByte();
        OwnerKey owner = new OwnerKey(OwnerKey.Type.values()[Math.max(0,
                Math.min(type, OwnerKey.Type.values().length - 1))], buffer.readUUID());
        return new PersonalDimCreateSubmitPacket(owner, buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt());
    }

    public static void handle(PersonalDimCreateSubmitPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            OwnerKey resolved = PersonalDimensionManager.resolveOwner(player);
            if (!resolved.equals(packet.owner)) {
                return;
            }
            if (!isValidColor(packet.roadColor) || !isValidColor(packet.lineColor)
                    || !isValidColor(packet.platformColor)) {
                return;
            }
            DimensionEntry entry = PersonalDimensionManager.getDimensionEntry(player.server, packet.owner);
            if (!entry.created) {
                PersonalDimensionManager.setColorScheme(player.server, packet.owner,
                        FloorColorScheme.ofConcrete(DyeColor.byId(packet.roadColor),
                                DyeColor.byId(packet.lineColor), DyeColor.byId(packet.platformColor)));
            }
            PersonalDimensionManager.setReturnPoint(player);
            ServerLevel level = PersonalDimensionManager.getOrCreateDimension(player.server, packet.owner);
            if (level == null) {
                return;
            }
            PersonalDimensionManager.teleportToDimension(player, packet.owner);
            player.closeContainer();
        });
        context.setPacketHandled(true);
    }

    private static boolean isValidColor(int id) {
        return id >= 0 && id < 16;
    }
}
