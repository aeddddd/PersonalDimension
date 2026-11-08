package com.github.aeddddd.personaldimension.network.packet;

import com.github.aeddddd.personaldimension.dimension.OwnerKey;
import com.github.aeddddd.personaldimension.dimension.PersonalDimensionManager;
import com.github.aeddddd.personaldimension.dimension.PersonalDimensionRules;
import com.github.aeddddd.personaldimension.dimension.rules.PlayerAbilityApplier;
import com.github.aeddddd.personaldimension.network.ModNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C→S：提交个人/队伍维度规则修改。
 */
public class PersonalDimRulesSubmitPacket {

    private final BlockPos pos;
    private final OwnerKey owner;
    private final PersonalDimensionRules rules;

    public PersonalDimRulesSubmitPacket(BlockPos pos, OwnerKey owner, PersonalDimensionRules rules) {
        this.pos = pos;
        this.owner = owner;
        this.rules = rules;
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

    public static void handle(PersonalDimRulesSubmitPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !PersonalDimensionManager.canManage(player, packet.owner)) {
                return;
            }
            PersonalDimensionRules rules = packet.rules.copy();
            rules.timeValue = Math.floorMod(rules.timeValue, 24000L);
            rules.movementSpeed = PlayerAbilityApplier.clampMovementSpeed(rules.movementSpeed);
            PersonalDimensionManager.setRules(player.server, packet.owner, rules);
            ModNetwork.CHANNEL.sendTo(
                    PersonalDimManagerStatePacket.create(player.server, packet.pos, packet.owner, player),
                    player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
        });
        context.setPacketHandled(true);
    }
}
