package com.github.aeddddd.personaldimension.network.packet;

import com.github.aeddddd.personaldimension.PersonalDimensionMod;
import com.github.aeddddd.personaldimension.client.handler.ClientPacketHandlers;
import com.github.aeddddd.personaldimension.dimension.PersonalDimensionRules;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S→C：同步玩家当前所在个人/队伍维度的规则；不在任何个人维度内时 {@code active=false}。
 */
public class PersonalDimRulesSyncPacket implements CustomPacketPayload {

    public static final Type<PersonalDimRulesSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PersonalDimensionMod.MOD_ID, "rules_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PersonalDimRulesSyncPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), PersonalDimRulesSyncPacket::decode);

    private final boolean active;
    private final PersonalDimensionRules rules;

    public PersonalDimRulesSyncPacket(boolean active, PersonalDimensionRules rules) {
        this.active = active;
        this.rules = rules;
    }

    @Override
    public Type<PersonalDimRulesSyncPacket> type() {
        return TYPE;
    }

    public static void encode(PersonalDimRulesSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.active);
        buffer.writeBoolean(packet.rules.disableMobSpawning);
        buffer.writeBoolean(packet.rules.lockWeather);
        buffer.writeBoolean(packet.rules.lockTime);
        buffer.writeBoolean(packet.rules.daylightCycle);
        buffer.writeLong(packet.rules.timeValue);
        buffer.writeBoolean(packet.rules.flightEnabled);
        buffer.writeFloat(packet.rules.movementSpeed);
        buffer.writeBoolean(packet.rules.noFlightInertia);
    }

    public static PersonalDimRulesSyncPacket decode(FriendlyByteBuf buffer) {
        boolean active = buffer.readBoolean();
        PersonalDimensionRules rules = new PersonalDimensionRules();
        rules.disableMobSpawning = buffer.readBoolean();
        rules.lockWeather = buffer.readBoolean();
        rules.lockTime = buffer.readBoolean();
        rules.daylightCycle = buffer.readBoolean();
        rules.timeValue = buffer.readLong();
        rules.flightEnabled = buffer.readBoolean();
        rules.movementSpeed = buffer.readFloat();
        rules.noFlightInertia = buffer.readBoolean();
        return new PersonalDimRulesSyncPacket(active, rules);
    }

    public static void handle(PersonalDimRulesSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientPacketHandlers.updatePersonalDimRules(packet.active, packet.rules);
            }
        });
    }
}
