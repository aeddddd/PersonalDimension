package com.github.aeddddd.personaldimension.client.handler;

import com.github.aeddddd.personaldimension.client.ClientPersonalDimRules;
import com.github.aeddddd.personaldimension.client.ClientPersonalDimState;
import com.github.aeddddd.personaldimension.dimension.PersonalDimensionRules;
import com.github.aeddddd.personaldimension.network.packet.PersonalDimManagerStatePacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

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
