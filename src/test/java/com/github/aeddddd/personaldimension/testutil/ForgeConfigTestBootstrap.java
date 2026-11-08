package com.github.aeddddd.personaldimension.testutil;

import com.github.aeddddd.personaldimension.PersonalDimensionConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.lang.reflect.Field;

/**
 * 单元测试环境下的配置引导工具。
 *
 * <p>NeoForge 1.21 的 {@code ModConfigSpec} 不再提供 {@code setConfig}，测试环境下也没有
 * 真实的 {@code ModConfig} 加载流程。这里直接把每个配置值的缓存字段填为默认值，足够覆盖
 * 只读配置的策略/管理器测试；幂等，可重复调用。</p>
 */
public final class ForgeConfigTestBootstrap {

    private static volatile boolean bootstrapped;

    private ForgeConfigTestBootstrap() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }
        synchronized (ForgeConfigTestBootstrap.class) {
            if (bootstrapped) {
                return;
            }
            try {
                Field cached = ModConfigSpec.ConfigValue.class.getDeclaredField("cachedValue");
                cached.setAccessible(true);
                for (Field field : PersonalDimensionConfig.Common.class.getDeclaredFields()) {
                    if (!ModConfigSpec.ConfigValue.class.isAssignableFrom(field.getType())) {
                        continue;
                    }
                    field.setAccessible(true);
                    ModConfigSpec.ConfigValue<?> value =
                            (ModConfigSpec.ConfigValue<?>) field.get(PersonalDimensionConfig.COMMON);
                    cached.set(value, value.getDefault());
                }
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("无法为测试填充配置默认值", e);
            }
            bootstrapped = true;
        }
    }
}
