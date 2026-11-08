package com.github.aeddddd.personalplane.dimension;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 个人维度数据的 SavedData，保存在主世界。
 *
 * <p>v1 格式：维度数据按 {@link OwnerKey} 存一份（玩家或队伍共享），玩家状态按 UUID 存一份
 * （模式与返回点）。不读取 AE2Enhanced 的旧存档格式。</p>
 */
public class PersonalDimensionData extends SavedData {

    private static final String DATA_NAME = "personal_plane_personal_planes";
    private static final int CURRENT_VERSION = 1;

    private final Map<OwnerKey, DimensionEntry> dimensions = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerDimState> players = new ConcurrentHashMap<>();

    public DimensionEntry getDimensionEntry(OwnerKey owner) {
        return dimensions.computeIfAbsent(owner, key -> new DimensionEntry());
    }

    @Nullable
    public DimensionEntry peekDimensionEntry(OwnerKey owner) {
        return dimensions.get(owner);
    }

    public Collection<DimensionEntry> getAllDimensionEntries() {
        return dimensions.values();
    }

    public Collection<Map.Entry<OwnerKey, DimensionEntry>> getAllDimensions() {
        return dimensions.entrySet();
    }

    /**
     * 是否已有任一维度被创建。供登录补建前快速短路。
     */
    public boolean hasAnyCreatedDimension() {
        for (DimensionEntry entry : dimensions.values()) {
            if (entry.created) {
                return true;
            }
        }
        return false;
    }

    public PlayerDimState getPlayerState(UUID playerId) {
        return players.computeIfAbsent(playerId, key -> new PlayerDimState());
    }

    @Nullable
    public PlayerDimState peekPlayerState(UUID playerId) {
        return players.get(playerId);
    }

    public void setRules(OwnerKey owner, PersonalDimensionRules rules) {
        getDimensionEntry(owner).rules = rules.copy();
        setDirty();
    }

    public void setColorScheme(OwnerKey owner, com.github.aeddddd.personalplane.api.dimension.FloorColorScheme scheme) {
        getDimensionEntry(owner).colorScheme = scheme.copy();
        setDirty();
    }

    public void setFloorLayout(OwnerKey owner, PersonalDimFloorLayout layout) {
        getDimensionEntry(owner).floorLayout = layout.copy();
        setDirty();
    }

    public void removeDimensionEntry(OwnerKey owner) {
        dimensions.remove(owner);
        setDirty();
    }

    public static PersonalDimensionData load(CompoundTag tag) {
        PersonalDimensionData data = new PersonalDimensionData();
        ListTag dimensions = tag.getList("dimensions", Tag.TAG_COMPOUND);
        for (int i = 0; i < dimensions.size(); i++) {
            CompoundTag entryTag = dimensions.getCompound(i);
            OwnerKey owner = OwnerKey.readFromNBT(entryTag.getCompound("owner"));
            if (owner == null) {
                continue;
            }
            DimensionEntry entry = new DimensionEntry();
            entry.readFromNBT(entryTag);
            data.dimensions.put(owner, entry);
        }
        ListTag players = tag.getList("players", Tag.TAG_COMPOUND);
        for (int i = 0; i < players.size(); i++) {
            CompoundTag stateTag = players.getCompound(i);
            if (!stateTag.hasUUID("uuid")) {
                continue;
            }
            PlayerDimState state = new PlayerDimState();
            state.readFromNBT(stateTag);
            data.players.put(stateTag.getUUID("uuid"), state);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("version", CURRENT_VERSION);
        ListTag dimensions = new ListTag();
        for (Map.Entry<OwnerKey, DimensionEntry> mapEntry : this.dimensions.entrySet()) {
            CompoundTag entryTag = mapEntry.getValue().writeToNBT();
            entryTag.put("owner", mapEntry.getKey().writeToNBT());
            dimensions.add(entryTag);
        }
        tag.put("dimensions", dimensions);

        ListTag players = new ListTag();
        for (Map.Entry<UUID, PlayerDimState> mapEntry : this.players.entrySet()) {
            CompoundTag stateTag = mapEntry.getValue().writeToNBT();
            stateTag.putUUID("uuid", mapEntry.getKey());
            players.add(stateTag);
        }
        tag.put("players", players);
        return tag;
    }

    public static PersonalDimensionData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                PersonalDimensionData::load, PersonalDimensionData::new, DATA_NAME);
    }
}
