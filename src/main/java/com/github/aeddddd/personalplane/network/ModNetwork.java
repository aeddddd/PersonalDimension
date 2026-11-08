package com.github.aeddddd.personalplane.network;

import com.github.aeddddd.personalplane.PersonalPlaneMod;
import com.github.aeddddd.personalplane.network.packet.PersonalDimColorSchemePacket;
import com.github.aeddddd.personalplane.network.packet.PersonalDimCreateSubmitPacket;
import com.github.aeddddd.personalplane.network.packet.PersonalDimFloorLayoutPacket;
import com.github.aeddddd.personalplane.network.packet.PersonalDimManagerStatePacket;
import com.github.aeddddd.personalplane.network.packet.PersonalDimModePacket;
import com.github.aeddddd.personalplane.network.packet.PersonalDimRulesSubmitPacket;
import com.github.aeddddd.personalplane.network.packet.PersonalDimRulesSyncPacket;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 网络载荷注册（NeoForge 1.21 PayloadRegistrar）。
 */
@EventBusSubscriber(modid = PersonalPlaneMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ModNetwork {

    private static final String VERSION = "1";

    private ModNetwork() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);

        registrar.playToClient(PersonalDimManagerStatePacket.TYPE,
                PersonalDimManagerStatePacket.STREAM_CODEC, PersonalDimManagerStatePacket::handle);
        registrar.playToClient(PersonalDimRulesSyncPacket.TYPE,
                PersonalDimRulesSyncPacket.STREAM_CODEC, PersonalDimRulesSyncPacket::handle);

        registrar.playToServer(PersonalDimRulesSubmitPacket.TYPE,
                PersonalDimRulesSubmitPacket.STREAM_CODEC, PersonalDimRulesSubmitPacket::handle);
        registrar.playToServer(PersonalDimColorSchemePacket.TYPE,
                PersonalDimColorSchemePacket.STREAM_CODEC, PersonalDimColorSchemePacket::handle);
        registrar.playToServer(PersonalDimCreateSubmitPacket.TYPE,
                PersonalDimCreateSubmitPacket.STREAM_CODEC, PersonalDimCreateSubmitPacket::handle);
        registrar.playToServer(PersonalDimFloorLayoutPacket.TYPE,
                PersonalDimFloorLayoutPacket.STREAM_CODEC, PersonalDimFloorLayoutPacket::handle);
        registrar.playToServer(PersonalDimModePacket.TYPE,
                PersonalDimModePacket.STREAM_CODEC, PersonalDimModePacket::handle);
    }
}
