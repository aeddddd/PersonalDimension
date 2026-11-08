package com.github.aeddddd.personaldimension.client;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;

import com.github.aeddddd.personaldimension.client.gui.PersonalDimensionManagerScreen;
import com.github.aeddddd.personaldimension.network.packet.PersonalDimManagerStatePacket;

/**
 * 客户端个人维度管理器状态缓存:收到服务端状态包后更新当前打开的界面.
 */
public final class ClientPersonalDimState {

    private ClientPersonalDimState() {
    }

    public static void update(PersonalDimManagerStatePacket packet) {
        if (Minecraft.getInstance().screen instanceof PersonalDimensionManagerScreen screen) {
            screen.updateState(packet);
        }
    }
}
