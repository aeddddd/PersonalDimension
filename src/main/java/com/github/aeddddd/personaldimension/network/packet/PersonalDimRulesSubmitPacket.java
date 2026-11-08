package com.github.aeddddd.personaldimension.network.packet;

import com.github.aeddddd.personaldimension.PersonalDimensionMod;
import com.github.aeddddd.personaldimension.dimension.OwnerKey;
import com.github.aeddddd.personaldimension.dimension.PersonalDimensionManager;
import com.github.aeddddd.personaldimension.dimension.PersonalDimensionRules;
import com.github.aeddddd.personaldimension.dimension.rules.PlayerAbilityApplier;
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
 * C→S：提交个人/队伍维度规则修改。
 */
public class PersonalDimRulesSubmitPacket implements CustomPacketPayload {

    public static final Type<PersonalDimRulesSubmitPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PersonalDimensionMod.MOD_ID, "rules_submit"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PersonalDimRulesSubmitPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), PersonalDimRulesSubmitPacket::decode);

    private final BlockPos pos;
    private final OwnerKey owner;
    private final PersonalDimensionRules rules;

    public PersonalDimRulesSubmitPacket(BlockPos pos, OwnerKey owner, PersonalDimensionRules rules) {
        this.pos = pos;
        this.owner = owner;
        this.rules = rules;
    }

    @Override
    public Type<PersonalDimRulesSubmitPacket> type() {
        return TYPE;
    }

    public static void encode(PersonalDimRulesSubmitPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeByte(packet.owner.type().ordinal());
        buffer.writeUUID(packet.owner.id());
        buffer.writeBoolean(packet.rules.disableMobSpawning);
        buffer.writeBoolean(packet.rules.lockWeather);
        buffer.writeBoolean(packet.rules.lockTime);
        buffer.writeBoolean(packet.rules.daylightCycle);
        buffer.writeLong(packet.rules.timeValue);
        buffer.writeBoolean(packet.rules.flightEnabled);
        buffer.writeFloat(packet.rules.movementSpeed);
        buffer.writeBoolean(packet.rules.noFlightInertia);
    }

    public static PersonalDimRulesSubmitPacket decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        int type = buffer.readByte();
        OwnerKey owner = new OwnerKey(OwnerKey.Type.values()[Math.max(0,
                Math.min(type, OwnerKey.Type.values().length - 1))], buffer.readUUID());
        PersonalDimensionRules rules = new PersonalDimensionRules();
        rules.disableMobSpawning = buffer.readBoolean();
        rules.lockWeather = buffer.readBoolean();
        rules.lockTime = buffer.readBoolean();
        rules.daylightCycle = buffer.readBoolean();
        rules.timeValue = buffer.readLong();
        rules.flightEnabled = buffer.readBoolean();
        rules.movementSpeed = buffer.readFloat();
        rules.noFlightInertia = buffer.readBoolean();
        return new PersonalDimRulesSubmitPacket(pos, owner, rules);
    }

    public static void handle(PersonalDimRulesSubmitPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)
                    || !PersonalDimensionManager.canManage(player, packet.owner)) {
                return;
            }
            PersonalDimensionRules rules = packet.rules.copy();
            rules.timeValue = Math.floorMod(rules.timeValue, 24000L);
            rules.movementSpeed = PlayerAbilityApplier.clampMovementSpeed(rules.movementSpeed);
            PersonalDimensionManager.setRules(player.server, packet.owner, rules);
            PacketDistributor.sendToPlayer(player,
                    PersonalDimManagerStatePacket.create(player.server, packet.pos, packet.owner, player));
        });
    }
}
