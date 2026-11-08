package com.github.aeddddd.personaldimension.access;

import com.github.aeddddd.personaldimension.PersonalDimensionMod;
import com.mojang.authlib.GameProfile;
import net.neoforged.fml.ModList;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

/**
 * FTB Ranks 的反射桥接。
 *
 * <p>只认显式 {@code true}；{@code false} 或未定义都返回 null，让调用方继续按 FTB Teams
 * 判定，避免默认拒绝误伤。FTB Ranks 缺席或版本不兼容时静默禁用。</p>
 */
public final class RanksAccess {

    private static volatile boolean resolved;
    private static volatile boolean available;
    private static boolean warned;

    @Nullable
    private static Method getPermissionValueMethod;

    public static boolean available() {
        if (!resolved) {
            resolve();
        }
        return available;
    }

    private static synchronized void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            if (!ModList.get().isLoaded("ftbranks")) {
                available = false;
                return;
            }
        } catch (Throwable t) {
            available = false;
            return;
        }
        try {
            Class<?> api = Class.forName("dev.ftb.mods.ftbranks.api.FTBRanksAPI");
            getPermissionValueMethod = findMethod(api, "getPermissionValue", GameProfile.class, String.class);
            if (getPermissionValueMethod == null) {
                getPermissionValueMethod = findMethod(api, "getPermissionValue", String.class);
            }
            available = getPermissionValueMethod != null;
            if (!available) {
                warn("FTB Ranks API methods not found; rank nodes disabled");
            }
        } catch (Throwable t) {
            available = false;
            warn("FTB Ranks API not compatible: " + t);
        }
    }

    @Nullable
    private static Method findMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        try {
            return type.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static void warn(String message) {
        if (!warned) {
            warned = true;
            PersonalDimensionMod.LOGGER.warn("[Pdim] {}", message);
        }
    }

    /**
     * 查询权限节点。
     *
     * @return {@link Boolean#TRUE} 表示显式授予；{@link Boolean#FALSE} 表示显式拒绝；
     *         {@code null} 表示未定义或 FTB Ranks 不可用
     */
    @Nullable
    public static Boolean permission(GameProfile profile, String node) {
        if (!available() || getPermissionValueMethod == null) {
            return null;
        }
        try {
            Object value = getPermissionValueMethod.getParameterCount() == 2
                    ? getPermissionValueMethod.invoke(null, profile, node)
                    : getPermissionValueMethod.invoke(null, node);
            if (value == null) {
                return null;
            }
            Method asBoolean = findMethod(value.getClass(), "asBoolean");
            if (asBoolean != null) {
                Object result = asBoolean.invoke(value);
                if (result instanceof Boolean bool) {
                    return bool;
                }
            }
            Method isTrue = findMethod(value.getClass(), "isTrue");
            if (isTrue != null) {
                Object result = isTrue.invoke(value);
                if (result instanceof Boolean bool) {
                    return bool;
                }
            }
            String text = value.toString();
            if ("true".equalsIgnoreCase(text)) {
                return Boolean.TRUE;
            }
            if ("false".equalsIgnoreCase(text)) {
                return Boolean.FALSE;
            }
            return null;
        } catch (Throwable t) {
            warn("Failed to query FTB Ranks node " + node + ": " + t);
            return null;
        }
    }

    public static boolean grants(GameProfile profile, String node) {
        return Boolean.TRUE.equals(permission(profile, node));
    }
}
