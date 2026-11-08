package com.github.aeddddd.personalplane.network;

import com.github.aeddddd.personalplane.PersonalPlaneMod;
import com.github.aeddddd.personalplane.network.packet.PersonalDimColorSchemePacket;
import com.github.aeddddd.personalplane.network.packet.PersonalDimCreateSubmitPacket;
import com.github.aeddddd.personalplane.network.packet.PersonalDimFloorLayoutPacket;
import com.github.aeddddd.personalplane.network.packet.PersonalDimManagerStatePacket;
import com.github.aeddddd.personalplane.network.packet.PersonalDimModePacket;
import com.github.aeddddd.personalplane.network.packet.PersonalDimRulesSubmitPacket;
import com.github.aeddddd.personalplane.network.packet.PersonalDimRulesSyncPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 网络包注册中心（Forge 1.20.1 SimpleChannel）。
 */
public final class ModNetwork {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(PersonalPlaneMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private static int packetId = 0;

    private ModNetwork() {
    }

    public static void init() {
        CHANNEL.messageBuilder(PersonalDimManagerStatePacket.class, nextId())
                .encoder(PersonalDimManagerStatePacket::encode)
                .decoder(PersonalDimManagerStatePacket::decode)
                .consumerMainThread(PersonalDimManagerStatePacket::handle)
                .add();

        CHANNEL.messageBuilder(PersonalDimRulesSubmitPacket.class, nextId())
                .encoder(PersonalDimRulesSubmitPacket::encode)
                .decoder(PersonalDimRulesSubmitPacket::decode)
                .consumerMainThread(PersonalDimRulesSubmitPacket::handle)
                .add();

        CHANNEL.messageBuilder(PersonalDimColorSchemePacket.class, nextId())
                .encoder(PersonalDimColorSchemePacket::encode)
                .decoder(PersonalDimColorSchemePacket::decode)
                .consumerMainThread(PersonalDimColorSchemePacket::handle)
                .add();

        CHANNEL.messageBuilder(PersonalDimCreateSubmitPacket.class, nextId())
                .encoder(PersonalDimCreateSubmitPacket::encode)
                .decoder(PersonalDimCreateSubmitPacket::decode)
                .consumerMainThread(PersonalDimCreateSubmitPacket::handle)
                .add();

        CHANNEL.messageBuilder(PersonalDimRulesSyncPacket.class, nextId())
                .encoder(PersonalDimRulesSyncPacket::encode)
                .decoder(PersonalDimRulesSyncPacket::decode)
                .consumerMainThread(PersonalDimRulesSyncPacket::handle)
                .add();

        CHANNEL.messageBuilder(PersonalDimFloorLayoutPacket.class, nextId())
                .encoder(PersonalDimFloorLayoutPacket::encode)
                .decoder(PersonalDimFloorLayoutPacket::decode)
                .consumerMainThread(PersonalDimFloorLayoutPacket::handle)
                .add();

        CHANNEL.messageBuilder(PersonalDimModePacket.class, nextId())
                .encoder(PersonalDimModePacket::encode)
                .decoder(PersonalDimModePacket::decode)
                .consumerMainThread(PersonalDimModePacket::handle)
                .add();
    }

    private static int nextId() {
        return packetId++;
    }
}
