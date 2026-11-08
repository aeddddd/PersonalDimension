package com.github.aeddddd.personalplane.testutil;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

/**
 * 单元测试环境下的原版引导工具。幂等，可重复调用。
 */
public final class MinecraftTestBootstrap {

    private static volatile boolean bootstrapped;

    private MinecraftTestBootstrap() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }
        synchronized (MinecraftTestBootstrap.class) {
            if (bootstrapped) {
                return;
            }
            SharedConstants.tryDetectVersion();
            Bootstrap.bootStrap();
            bootstrapped = true;
        }
    }
}
