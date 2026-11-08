package com.github.aeddddd.personalplane.client.handler;

import com.github.aeddddd.personalplane.client.ClientPersonalDimRules;
import com.github.aeddddd.personalplane.client.ClientPersonalDimState;
import com.github.aeddddd.personalplane.dimension.PersonalDimensionRules;
import com.github.aeddddd.personalplane.network.packet.PersonalDimManagerStatePacket;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 网络包客户端处理入口。
 */
@OnlyIn(Dist.CLIENT)
public final class ClientPacketHandlers {

    private ClientPacketHandlers() {
    }

    public static void updatePersonalDimRules(boolean active, PersonalDimensionRules rules) {
        ClientPersonalDimRules.update(active ? rules : null);
    }

    public static void updatePersonalDimState(PersonalDimManagerStatePacket packet) {
        ClientPersonalDimState.update(packet);
    }
}
