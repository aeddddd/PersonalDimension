package com.github.aeddddd.personalplane.network.packet;

import com.github.aeddddd.personalplane.client.handler.ClientPacketHandlers;
import com.github.aeddddd.personalplane.dimension.PersonalDimensionRules;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S→C：同步玩家当前所在个人/队伍维度的规则；不在任何个人维度内时 {@code active=false}。
 */
public class PersonalDimRulesSyncPacket {

    private final boolean active;
    private final PersonalDimensionRules rules;

    public PersonalDimRulesSyncPacket(boolean active, PersonalDimensionRules rules) {
        this.active = active;
        this.rules = rules;
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

    public static void handle(PersonalDimRulesSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientPacketHandlers.updatePersonalDimRules(packet.active, packet.rules);
            }
        });
        context.setPacketHandled(true);
    }
}
