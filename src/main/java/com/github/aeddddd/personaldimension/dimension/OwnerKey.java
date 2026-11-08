package com.github.aeddddd.personaldimension.dimension;

import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 个人维度的归属键：玩家个人维度或 FTB 队伍维度。
 *
 * <p>维度键、存档数据与权限判定都以本键为唯一标识。队伍维度的 {@code id} 为
 * {@code Team#getTeamId()}；个人维度的 {@code id} 为玩家 UUID。</p>
 */
public record OwnerKey(Type type, UUID id) {

    public enum Type {
        PLAYER,
        TEAM
    }

    public OwnerKey {
        if (type == null || id == null) {
            throw new IllegalArgumentException("OwnerKey type and id must not be null");
        }
    }

    public static OwnerKey player(UUID playerId) {
        return new OwnerKey(Type.PLAYER, playerId);
    }

    public static OwnerKey team(UUID teamId) {
        return new OwnerKey(Type.TEAM, teamId);
    }

    public boolean isPlayer() {
        return type == Type.PLAYER;
    }

    public boolean isTeam() {
        return type == Type.TEAM;
    }

    public String serializedName() {
        return (type == Type.PLAYER ? "player:" : "team:") + id;
    }

    @Nullable
    public static OwnerKey parse(String value) {
        if (value == null) {
            return null;
        }
        int split = value.indexOf(':');
        if (split <= 0) {
            return null;
        }
        String type = value.substring(0, split);
        try {
            UUID id = UUID.fromString(value.substring(split + 1));
            return switch (type) {
                case "player" -> player(id);
                case "team" -> team(id);
                default -> null;
            };
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public CompoundTag writeToNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", type.name());
        tag.putUUID("id", id);
        return tag;
    }

    @Nullable
    public static OwnerKey readFromNBT(CompoundTag tag) {
        if (!tag.contains("type") || !tag.hasUUID("id")) {
            return null;
        }
        try {
            return new OwnerKey(Type.valueOf(tag.getString("type")), tag.getUUID("id"));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
