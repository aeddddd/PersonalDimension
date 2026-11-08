package com.github.aeddddd.personalplane.testutil;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import com.github.aeddddd.personalplane.PersonalDimensionConfig;

/**
 * 单元测试环境下的 Forge 配置引导工具。幂等，可重复调用。
 */
public final class ForgeConfigTestBootstrap {

    private static volatile boolean bootstrapped;

    private ForgeConfigTestBootstrap() {
    }

    public static void bootstrap() {
        bootstrap("");
    }

    public static synchronized void bootstrap(String tomlContent) {
        if (bootstrapped) {
            return;
        }
        CommentedConfig config = new TomlParser().parse(tomlContent);
        PersonalDimensionConfig.COMMON_SPEC.setConfig(config);
        bootstrapped = true;
    }
}
