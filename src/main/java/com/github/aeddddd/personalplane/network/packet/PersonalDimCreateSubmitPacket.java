package com.github.aeddddd.personalplane.network.packet;

import com.github.aeddddd.personalplane.PersonalPlaneMod;
import com.github.aeddddd.personalplane.api.dimension.FloorColorScheme;
import com.github.aeddddd.personalplane.dimension.DimensionEntry;
import com.github.aeddddd.personalplane.dimension.OwnerKey;
import com.github.aeddddd.personalplane.dimension.PersonalDimensionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C→S：创建向导确认提交，按所选颜色方案创建当前模式对应的维度并进入。
 */
public class PersonalDimCreateSubmitPacket implements CustomPacketPayload {

    public static final Type<PersonalDimCreateSubmitPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PersonalPlaneMod.MOD_ID, "create_submit"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PersonalDimCreateSubmitPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), PersonalDimCreateSubmitPacket::decode);

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

    @Override
    public Type<PersonalDimCreateSubmitPacket> type() {
        return TYPE;
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

    public static void handle(PersonalDimCreateSubmitPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
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
    }

    private static boolean isValidColor(int id) {
        return id >= 0 && id < 16;
    }
}
