package com.github.aeddddd.personalplane.access;

import com.github.aeddddd.personalplane.PersonalPlaneMod;
import net.minecraftforge.fml.ModList;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * FTB Teams 的反射桥接。
 *
 * <p>本模组不把 FTB 作为编译期依赖：FTB 缺席时 {@link #available()} 返回 false，
 * 由 {@link AccessPolicies} 退化到 {@link FallbackAccessPolicy}。FTB 版本差异或
 * 队伍数据异常时也只记录一次警告并返回空结果，不向事件总线抛异常。</p>
 */
public final class FtbTeamAccess implements TeamAccess {

    private static volatile boolean resolved;
    private static volatile boolean available;
    private static boolean warned;

    private static Class<?> apiClass;
    private static Class<?> apiInterfaceClass;
    private static Class<?> managerClass;
    private static Class<?> teamClass;
    private static Method apiMethod;
    private static Method getManagerMethod;
    @Nullable
    private static Method getTeamForPlayerIdMethod;
    @Nullable
    private static Method getTeamByIdMethod;
    @Nullable
    private static Method getTeamIdMethod;
    @Nullable
    private static Method getIdMethod;
    @Nullable
    private static Method isPlayerTeamMethod;
    @Nullable
    private static Method getRankForPlayerMethod;
    @Nullable
    private static Method getShortNameMethod;
    @Nullable
    private static Method getNameMethod;
    @Nullable
    private static Method getMembersMethod;
    @Nullable
    private static Method isValidMethod;

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
            if (!ModList.get().isLoaded("ftbteams")) {
                available = false;
                return;
            }
        } catch (Throwable t) {
            available = false;
            return;
        }
        try {
            apiClass = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI");
            apiInterfaceClass = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI$API");
            managerClass = Class.forName("dev.ftb.mods.ftbteams.api.TeamManager");
            teamClass = Class.forName("dev.ftb.mods.ftbteams.api.Team");

            apiMethod = apiClass.getMethod("api");
            getManagerMethod = apiInterfaceClass.getMethod("getManager");
            getTeamForPlayerIdMethod = managerClass.getMethod("getTeamForPlayerID", UUID.class);
            getTeamByIdMethod = findMethod(managerClass, "getTeamByID", UUID.class);
            getTeamIdMethod = findMethod(teamClass, "getTeamId");
            getIdMethod = findMethod(teamClass, "getId");
            isPlayerTeamMethod = findMethod(teamClass, "isPlayerTeam");
            getRankForPlayerMethod = findMethod(teamClass, "getRankForPlayer", UUID.class);
            getShortNameMethod = findMethod(teamClass, "getShortName");
            getNameMethod = findMethod(teamClass, "getName");
            getMembersMethod = findMethod(teamClass, "getMembers");
            isValidMethod = findMethod(teamClass, "isValid");

            available = getTeamForPlayerIdMethod != null && (getTeamIdMethod != null || getIdMethod != null);
            if (!available) {
                warn("FTB Teams API methods not found; falling back to owner/OP access");
            }
        } catch (Throwable t) {
            available = false;
            warn("FTB Teams API not compatible: " + t);
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
            PersonalPlaneMod.LOGGER.warn("[Pdim] {}", message);
        }
    }

    @Nullable
    private static Object manager() {
        if (!available()) {
            return null;
        }
        try {
            Object api = apiMethod.invoke(null);
            return getManagerMethod.invoke(api);
        } catch (Throwable t) {
            warn("Failed to access FTB Teams manager: " + t);
            return null;
        }
    }

    @Nullable
    private static Object unwrapTeam(@Nullable Object value) {
        if (value instanceof Optional<?> optional) {
            return optional.orElse(null);
        }
        return value;
    }

    @Nullable
    private static Object teamById(UUID teamId) {
        Object manager = manager();
        if (manager == null || getTeamByIdMethod == null) {
            return null;
        }
        try {
            return unwrapTeam(getTeamByIdMethod.invoke(manager, teamId));
        } catch (Throwable t) {
            warn("Failed to query FTB team " + teamId + ": " + t);
            return null;
        }
    }

    @Nullable
    private static UUID teamId(Object team) {
        try {
            Object value = getTeamIdMethod != null ? getTeamIdMethod.invoke(team) : getIdMethod.invoke(team);
            return value instanceof UUID uuid ? uuid : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean isPersonalTeam(Object team) {
        if (isPlayerTeamMethod == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(isPlayerTeamMethod.invoke(team));
        } catch (Throwable t) {
            return false;
        }
    }

    @Nullable
    private static String nameOf(Object team) {
        try {
            if (getShortNameMethod != null) {
                Object value = getShortNameMethod.invoke(team);
                if (value instanceof String name && !name.isEmpty()) {
                    return name;
                }
            }
            if (getNameMethod != null) {
                Object value = getNameMethod.invoke(team);
                if (value instanceof net.minecraft.network.chat.Component component) {
                    return component.getString();
                }
                if (value != null) {
                    return value.toString();
                }
            }
        } catch (Throwable ignored) {
            // 显示名失败不影响权限判定
        }
        return "";
    }

    @Override
    public Optional<TeamRef> teamOf(UUID playerId) {
        if (!available()) {
            return Optional.empty();
        }
        Object manager = manager();
        if (manager == null) {
            return Optional.empty();
        }
        try {
            Object result = getTeamForPlayerIdMethod.invoke(manager, playerId);
            Object team = unwrapTeam(result);
            if (team == null) {
                return Optional.empty();
            }
            if (isValidMethod != null && !Boolean.TRUE.equals(isValidMethod.invoke(team))) {
                return Optional.empty();
            }
            UUID id = teamId(team);
            if (id == null) {
                return Optional.empty();
            }
            return Optional.of(new TeamRef(id, nameOf(team), isPersonalTeam(team)));
        } catch (Throwable t) {
            warn("Failed to query FTB team of " + playerId + ": " + t);
            return Optional.empty();
        }
    }

    @Override
    public Rank rankOf(UUID playerId, UUID teamId) {
        Object team = teamById(teamId);
        if (team == null || getRankForPlayerMethod == null) {
            return Rank.UNKNOWN;
        }
        try {
            Object value = getRankForPlayerMethod.invoke(team, playerId);
            if (value instanceof Enum<?> rank) {
                try {
                    return Rank.valueOf(rank.name());
                } catch (IllegalArgumentException e) {
                    return Rank.UNKNOWN;
                }
            }
            return Rank.UNKNOWN;
        } catch (Throwable t) {
            warn("Failed to query FTB rank of " + playerId + " in " + teamId + ": " + t);
            return Rank.UNKNOWN;
        }
    }

    @Override
    public String teamName(UUID teamId) {
        Object team = teamById(teamId);
        return team != null ? nameOf(team) : "";
    }

    @Override
    public int memberCount(UUID teamId) {
        Object team = teamById(teamId);
        if (team == null || getMembersMethod == null) {
            return 0;
        }
        try {
            Object value = getMembersMethod.invoke(team);
            return value instanceof Set<?> set ? set.size() : 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    @Override
    public boolean teamExists(UUID teamId) {
        Object team = teamById(teamId);
        if (team == null) {
            return false;
        }
        try {
            return isValidMethod == null || Boolean.TRUE.equals(isValidMethod.invoke(team));
        } catch (Throwable t) {
            return false;
        }
    }
}
