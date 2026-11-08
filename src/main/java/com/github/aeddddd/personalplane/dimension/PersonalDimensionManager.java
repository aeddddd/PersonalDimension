package com.github.aeddddd.personalplane.dimension;

import com.github.aeddddd.personalplane.PersonalDimensionConfig;
import com.github.aeddddd.personalplane.PersonalPlaneMod;
import com.github.aeddddd.personalplane.access.AccessAction;
import com.github.aeddddd.personalplane.access.AccessPolicies;
import com.github.aeddddd.personalplane.api.dimension.FloorColorScheme;
import com.github.aeddddd.personalplane.api.dimension.IFloorPreset;
import com.github.aeddddd.personalplane.dimension.rules.PlayerAbilityApplier;
import com.github.aeddddd.personalplane.mixin.accessor.MinecraftServerAccessor;
import com.github.aeddddd.personalplane.network.ModNetwork;
import com.github.aeddddd.personalplane.network.packet.PersonalDimRulesSyncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkDirection;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * 个人维度管理器：按玩家或 FTB 队伍动态创建维度、传送、执行规则与访问控制。
 *
 * <p>维度键由 {@link OwnerKey} 决定：玩家维度 {@code personal_plane:pd_<uuid>}，
 * 队伍维度 {@code personal_plane:td_<teamid>}。队伍维度的规则、配色、布局与入口点
 * 由全队共享；玩家返回点与模式保存在 {@link PlayerDimState}。</p>
 */
public final class PersonalDimensionManager {

    private PersonalDimensionManager() {
    }

    private static final String PLAYER_PREFIX = "pd_";
    private static final String TEAM_PREFIX = "td_";

    private static final Pattern UUID_HEX_PATTERN = Pattern.compile(
            "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})");

    private static final ResourceKey<DimensionType> PERSONAL_DIM_TYPE = ResourceKey.create(
            Registries.DIMENSION_TYPE, new ResourceLocation(PersonalPlaneMod.MOD_ID, "personal_dim"));

    private static final ChunkProgressListener NOOP_PROGRESS_LISTENER = new ChunkProgressListener() {
        @Override
        public void updateSpawnPos(ChunkPos pos) {
        }

        @Override
        public void onStatusChange(ChunkPos pos, @Nullable ChunkStatus status) {
        }

        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }
    };

    // ==================== 维度键与归属 ====================

    public static OwnerKey resolveOwner(ServerPlayer player) {
        return OwnerResolver.resolve(player);
    }

    public static OwnerKey resolveOwner(MinecraftServer server, UUID playerId) {
        return OwnerResolver.resolve(server, playerId);
    }

    public static ResourceKey<Level> dimensionKeyFor(OwnerKey owner) {
        String prefix = owner.isPlayer() ? PLAYER_PREFIX : TEAM_PREFIX;
        return ResourceKey.create(Registries.DIMENSION, new ResourceLocation(PersonalPlaneMod.MOD_ID,
                prefix + owner.id().toString().replace("-", "")));
    }

    public static boolean isPersonalDimension(ResourceKey<Level> key) {
        ResourceLocation location = key.location();
        if (!PersonalPlaneMod.MOD_ID.equals(location.getNamespace())) {
            return false;
        }
        String path = location.getPath();
        return path.startsWith(PLAYER_PREFIX) || path.startsWith(TEAM_PREFIX);
    }

    public static boolean isPersonalDimension(Level level) {
        return isPersonalDimension(level.dimension());
    }

    @Nullable
    public static OwnerKey ownerFromKey(ResourceKey<Level> key) {
        if (!isPersonalDimension(key)) {
            return null;
        }
        String path = key.location().getPath();
        boolean team = path.startsWith(TEAM_PREFIX);
        String hex = path.substring((team ? TEAM_PREFIX : PLAYER_PREFIX).length());
        try {
            UUID id = UUID.fromString(UUID_HEX_PATTERN.matcher(hex).replaceFirst("$1-$2-$3-$4-$5"));
            return team ? OwnerKey.team(id) : OwnerKey.player(id);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ==================== 数据访问 ====================

    public static DimensionEntry getDimensionEntry(MinecraftServer server, OwnerKey owner) {
        return PersonalDimensionData.get(server).getDimensionEntry(owner);
    }

    public static PlayerDimState getPlayerState(MinecraftServer server, UUID playerId) {
        return PersonalDimensionData.get(server).getPlayerState(playerId);
    }

    @Nullable
    public static DimensionEntry getEntryByDimension(MinecraftServer server, ResourceKey<Level> key) {
        OwnerKey owner = ownerFromKey(key);
        return owner != null ? PersonalDimensionData.get(server).peekDimensionEntry(owner) : null;
    }

    public static void setEntryPoint(MinecraftServer server, OwnerKey owner, BlockPos pos) {
        DimensionEntry entry = getDimensionEntry(server, owner);
        entry.entryPoint = pos;
        PersonalDimensionData.get(server).setDirty();
    }

    public static void setReturnPoint(ServerPlayer player) {
        PersonalDimensionData data = PersonalDimensionData.get(player.server);
        PlayerDimState state = data.getPlayerState(player.getUUID());
        state.returnDim = player.level().dimension().location().toString();
        state.returnX = player.getX();
        state.returnY = player.getY();
        state.returnZ = player.getZ();
        state.returnYaw = player.getYRot();
        state.returnPitch = player.getXRot();
        state.hasReturnPoint = true;
        data.setDirty();
    }

    public static void setMode(MinecraftServer server, UUID playerId, DimensionMode mode) {
        PersonalDimensionData data = PersonalDimensionData.get(server);
        data.getPlayerState(playerId).mode = mode;
        data.setDirty();
    }

    public static DimensionMode getMode(MinecraftServer server, UUID playerId) {
        return PersonalDimensionData.get(server).getPlayerState(playerId).mode;
    }

    // ==================== 维度创建 ====================

    public static ServerLevel getOrCreateDimension(ServerPlayer player) {
        return getOrCreateDimension(player.server, resolveOwner(player));
    }

    public static synchronized ServerLevel getOrCreateDimension(MinecraftServer server, OwnerKey owner) {
        ResourceKey<Level> key = dimensionKeyFor(owner);
        ServerLevel existing = server.getLevel(key);
        if (existing != null) {
            return existing;
        }
        ServerLevel created = createDimension(server, owner);
        if (created != null) {
            DimensionEntry entry = getDimensionEntry(server, owner);
            entry.created = true;
            PersonalDimensionData.get(server).setDirty();
            PersonalPlaneMod.LOGGER.info("[Pdim] Created {} dimension {}",
                    owner.isTeam() ? "team" : "personal", key.location());
        }
        return created;
    }

    @Nullable
    private static ServerLevel createDimension(MinecraftServer server, OwnerKey owner) {
        ResourceKey<Level> key = dimensionKeyFor(owner);
        try {
            RegistryAccess registryAccess = server.registryAccess();
            Holder<DimensionType> typeHolder = registryAccess.registryOrThrow(Registries.DIMENSION_TYPE)
                    .getHolderOrThrow(PERSONAL_DIM_TYPE);
            Holder<Biome> plains = registryAccess.registryOrThrow(Registries.BIOME)
                    .getHolderOrThrow(Biomes.PLAINS);

            ChunkGenerator generator = new PersonalDimChunkGenerator(new FixedBiomeSource(plains),
                    PersonalDimensionConfig.COMMON.personalDimensionFloorY.get());
            LevelStem stem = new LevelStem(typeHolder, generator);
            PersonalDimLevelData levelData = new PersonalDimLevelData(server.getWorldData().overworldData());
            levelData.readFrom(getDimensionEntry(server, owner));
            long biomeZoomSeed = BiomeManager.obfuscateSeed(server.getWorldData().worldGenOptions().seed());

            MinecraftServerAccessor accessor = (MinecraftServerAccessor) server;
            ServerLevel level = new ServerLevel(server, accessor.getExecutor(), accessor.getStorageSource(),
                    levelData, key, stem, NOOP_PROGRESS_LISTENER, false, biomeZoomSeed,
                    List.of(), true, server.overworld().getRandomSequences());

            accessor.getLevels().put(key, level);
            server.markWorldsDirty();
            BorderChangeListener borderListener =
                    new BorderChangeListener.DelegateBorderChangeListener(level.getWorldBorder());
            server.overworld().getWorldBorder().addListener(borderListener);
            BORDER_LISTENERS.put(key, borderListener);
            MinecraftForge.EVENT_BUS.post(new LevelEvent.Load(level));
            return level;
        } catch (Exception e) {
            PersonalPlaneMod.LOGGER.error("[Pdim] Failed to create dimension {}", key.location(), e);
            return null;
        }
    }

    /**
     * 玩家登录读取存档后、维度解析前补建其所在个人/队伍维度。
     */
    @SubscribeEvent
    public static void onPlayerLoading(PlayerEvent.LoadFromFile event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MinecraftServer server = player.server;
        PersonalDimensionData data = PersonalDimensionData.get(server);
        if (!data.hasAnyCreatedDimension()) {
            return;
        }
        ResourceKey<Level> key = readSavedDimension(event);
        if (key == null || !isPersonalDimension(key)) {
            return;
        }
        OwnerKey owner = ownerFromKey(key);
        if (owner == null) {
            return;
        }
        DimensionEntry entry = data.peekDimensionEntry(owner);
        if (entry == null || !entry.created) {
            return;
        }
        if (getOrCreateDimension(server, owner) == null) {
            PersonalPlaneMod.LOGGER.error("[Pdim] Saved dimension {} for player {} could not be recreated.",
                    key.location(), player.getUUID());
        }
    }

    @Nullable
    private static ResourceKey<Level> readSavedDimension(PlayerEvent.LoadFromFile event) {
        File file = new File(event.getPlayerDirectory(), event.getPlayerUUID() + ".dat");
        if (!file.isFile()) {
            return null;
        }
        try {
            CompoundTag tag = NbtIo.readCompressed(file);
            ResourceLocation location = ResourceLocation.tryParse(tag.getString("Dimension"));
            return location != null ? ResourceKey.create(Registries.DIMENSION, location) : null;
        } catch (IOException e) {
            PersonalPlaneMod.LOGGER.warn("[Pdim] Failed to read saved dimension of player {}",
                    event.getPlayerUUID(), e);
            return null;
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        MinecraftServer server = event.getServer();
        boolean dirty = false;
        for (ServerLevel level : server.getAllLevels()) {
            if (!isPersonalDimension(level.dimension())) {
                continue;
            }
            OwnerKey owner = ownerFromKey(level.dimension());
            if (owner == null || !(level.getLevelData() instanceof PersonalDimLevelData levelData)) {
                continue;
            }
            levelData.writeTo(getDimensionEntry(server, owner));
            dirty = true;
        }
        if (dirty) {
            PersonalDimensionData.get(server).setDirty();
        }
        BORDER_LISTENERS.clear();
        PENDING_RECARPET.clear();
        RECARPET_TASKS.clear();
        RECARPET_COOLDOWN.clear();
        DENY_MESSAGE_COOLDOWN.clear();
        TEAM_ACCESS_RECHECK.clear();
    }

    // ==================== 传送 ====================

    public static void teleportToDimension(ServerPlayer player, OwnerKey owner) {
        ResourceKey<Level> key = dimensionKeyFor(owner);
        ServerLevel target = player.server.getLevel(key);
        if (target == null) {
            return;
        }
        DimensionEntry entry = getDimensionEntry(player.server, owner);
        BlockPos entryPos = entry.entryPoint;
        int minY = PersonalDimensionConfig.COMMON.personalDimensionFloorY.get() + 1;
        if (entryPos.getY() < minY) {
            entryPos = new BlockPos(entryPos.getX(), Math.max(entryPos.getY(),
                    PersonalDimensionConfig.COMMON.personalDimensionEntryY.get()), entryPos.getZ());
        }
        BlockPos safePos = findSafeLandingPos(target, entryPos, minY);
        player.teleportTo(target, safePos.getX() + 0.5, safePos.getY() + 0.1, safePos.getZ() + 0.5,
                player.getYRot(), player.getXRot());
    }

    private static BlockPos findSafeLandingPos(ServerLevel level, BlockPos base, int minY) {
        BlockPos.MutableBlockPos pos = base.mutable();
        if (pos.getY() < minY) {
            pos.setY(minY);
        }
        int maxY = level.getMaxBuildHeight() - 2;
        while (pos.getY() < maxY
                && !(level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir())) {
            pos.move(Direction.UP);
        }
        return pos.immutable();
    }

    public static void teleportToReturnPoint(ServerPlayer player) {
        PersonalDimensionData data = PersonalDimensionData.get(player.server);
        PlayerDimState state = data.peekPlayerState(player.getUUID());
        if (state == null || !state.hasReturnPoint) {
            ServerLevel overworld = player.server.overworld();
            BlockPos spawn = overworld.getSharedSpawnPos();
            player.teleportTo(overworld, spawn.getX() + 0.5, spawn.getY() + 0.1, spawn.getZ() + 0.5,
                    player.getYRot(), player.getXRot());
            PlayerAbilityApplier.resetAbilities(player);
            return;
        }
        ResourceLocation dimId = ResourceLocation.tryParse(state.returnDim);
        ServerLevel target = dimId != null
                ? player.server.getLevel(ResourceKey.create(Registries.DIMENSION, dimId))
                : null;
        if (target == null) {
            target = player.server.overworld();
        }
        player.teleportTo(target, state.returnX, state.returnY, state.returnZ, state.returnYaw, state.returnPitch);
        PlayerAbilityApplier.resetAbilities(player);
    }

    /**
     * 将玩家传送到指定归属的维度，并校验 ENTER 权限。
     */
    public static boolean teleportPlayerToDimension(ServerPlayer player, OwnerKey owner) {
        if (!AccessPolicies.current().can(player, owner, AccessAction.ENTER)) {
            return false;
        }
        ResourceKey<Level> key = dimensionKeyFor(owner);
        if (player.server.getLevel(key) == null && createDimension(player.server, owner) == null) {
            PersonalPlaneMod.LOGGER.warn("[Pdim] Dimension {} could not be created, cannot teleport {}",
                    key.location(), player.getName().getString());
            return false;
        }
        // createDimension 成功后补写 created 标记
        DimensionEntry entry = getDimensionEntry(player.server, owner);
        if (!entry.created) {
            entry.created = true;
            PersonalDimensionData.get(player.server).setDirty();
        }
        teleportToDimension(player, owner);
        return true;
    }

    // ==================== 权限 ====================

    public static boolean canManage(ServerPlayer player, OwnerKey owner) {
        return AccessPolicies.current().can(player, owner, AccessAction.MANAGE);
    }

    /**
     * 判断玩家是否可以在个人/队伍维度内行使某项权限。
     * 非个人维度、维度数据缺失与 FakePlayer 一律放行。
     */
    public static boolean hasPermission(ServerPlayer player, ResourceKey<Level> dimension, AccessAction action) {
        if (player instanceof FakePlayer) {
            return true;
        }
        if (!isPersonalDimension(dimension)) {
            return true;
        }
        OwnerKey owner = ownerFromKey(dimension);
        if (owner == null) {
            return true;
        }
        if (getEntryByDimension(player.server, dimension) == null) {
            return true;
        }
        return AccessPolicies.current().can(player, owner, action);
    }

    // ==================== 规则 ====================

    public static void setRules(MinecraftServer server, OwnerKey owner, PersonalDimensionRules rules) {
        PersonalDimensionData.get(server).setRules(owner, rules);
        ServerLevel level = server.getLevel(dimensionKeyFor(owner));
        if (level != null) {
            for (ServerPlayer player : level.players()) {
                PlayerAbilityApplier.applyCapabilities(player, rules);
                sendRulesSync(player, rules);
            }
        }
    }

    private static void applyDimensionRules(ServerPlayer player) {
        DimensionEntry entry = getEntryByDimension(player.server, player.level().dimension());
        if (entry != null) {
            PlayerAbilityApplier.applyCapabilities(player, entry.rules);
            sendRulesSync(player, entry.rules);
        } else {
            PlayerAbilityApplier.resetAbilities(player);
            sendRulesSync(player, null);
        }
    }

    private static void sendRulesSync(ServerPlayer player, @Nullable PersonalDimensionRules rules) {
        ModNetwork.CHANNEL.sendTo(
                new PersonalDimRulesSyncPacket(rules != null, rules != null ? rules : new PersonalDimensionRules()),
                player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    // ==================== 颜色与布局 ====================

    public static void setColorScheme(MinecraftServer server, OwnerKey owner, FloorColorScheme scheme) {
        PersonalDimensionData.get(server).setColorScheme(owner, scheme);
    }

    public static void setFloorLayout(MinecraftServer server, OwnerKey owner, PersonalDimFloorLayout layout) {
        PersonalDimFloorLayout clamped = layout.copy();
        clamped.clamp(PersonalDimensionConfig.COMMON.personalDimensionFloorY.get());
        PersonalDimensionData.get(server).setFloorLayout(owner, clamped);
    }

    // ==================== 异步重铺 ====================

    private static final int RECARPET_CHUNKS_PER_TICK = 8;
    private static final long RECARPET_COOLDOWN_TICKS = 20L * 20L;
    private static final int RECARPET_COMPUTE_AHEAD = 64;

    private static final Map<ResourceKey<Level>, BorderChangeListener> BORDER_LISTENERS = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<Level>, Queue<ChunkPos>> PENDING_RECARPET = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<Level>, RecarpetTask> RECARPET_TASKS = new ConcurrentHashMap<>();
    private static final Map<OwnerKey, Long> RECARPET_COOLDOWN = new ConcurrentHashMap<>();

    private static final ExecutorService RECARPET_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Pdim-Recarpet");
        thread.setDaemon(true);
        return thread;
    });

    private static final class RecarpetTask {
        final IFloorPreset preset;
        final FloorColorScheme scheme;
        final int floorY;
        final int floors;
        final int clearUpTo;
        final Queue<ComputedChunk> computed = new ConcurrentLinkedQueue<>();
        final AtomicInteger inFlight = new AtomicInteger();

        RecarpetTask(IFloorPreset preset, FloorColorScheme scheme, int floorY, int floors, int clearUpTo) {
            this.preset = preset;
            this.scheme = scheme;
            this.floorY = floorY;
            this.floors = floors;
            this.clearUpTo = clearUpTo;
        }

        boolean isFinished(Queue<ChunkPos> pending) {
            return pending.isEmpty() && computed.isEmpty() && inFlight.get() == 0;
        }
    }

    private record ComputedChunk(ChunkPos pos, BlockState[] desired) {
    }

    public static int recarpetFloor(MinecraftServer server, OwnerKey owner) {
        DimensionEntry entry = getDimensionEntry(server, owner);
        int floorY = PersonalDimensionConfig.COMMON.personalDimensionFloorY.get();
        return recarpetFloor(server, owner, entry.floorLayout.clampedFloorCount(floorY));
    }

    public static int recarpetFloor(MinecraftServer server, OwnerKey owner, int clearUpToFloors) {
        DimensionEntry entry = PersonalDimensionData.get(server).peekDimensionEntry(owner);
        if (entry == null || !entry.created) {
            return 0;
        }
        ResourceKey<Level> key = dimensionKeyFor(owner);
        ServerLevel level = server.getLevel(key);
        if (level == null) {
            level = getOrCreateDimension(server, owner);
        }
        if (level == null) {
            return 0;
        }
        long now = server.overworld().getGameTime();
        Long last = RECARPET_COOLDOWN.get(owner);
        if (PENDING_RECARPET.containsKey(key) || (last != null && now - last < RECARPET_COOLDOWN_TICKS)) {
            return -1;
        }
        Path regionDir = ((MinecraftServerAccessor) server).getStorageSource()
                .getDimensionPath(key).resolve("region");
        List<ChunkPos> positions = listGeneratedChunks(regionDir);
        if (positions.isEmpty()) {
            return 0;
        }
        int floorY = PersonalDimensionConfig.COMMON.personalDimensionFloorY.get();
        int floors = entry.floorLayout.clampedFloorCount(floorY);
        IFloorPreset preset = ModFloorStyles.applyLayout(PresetLoader.getPreset(), entry.floorLayout);
        RecarpetTask task = new RecarpetTask(preset, entry.colorScheme.copy(), floorY, floors,
                Mth.clamp(clearUpToFloors, floors, PersonalDimFloorLayout.maxFloorCount(floorY)));
        PENDING_RECARPET.put(key, new ArrayDeque<>(positions));
        RECARPET_TASKS.put(key, task);
        RECARPET_COOLDOWN.put(owner, now);
        PersonalPlaneMod.LOGGER.info("[Pdim] Queued {} chunks for recarpet in {}",
                positions.size(), key.location());
        return positions.size();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING_RECARPET.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        for (var iterator = PENDING_RECARPET.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<ResourceKey<Level>, Queue<ChunkPos>> pendingEntry = iterator.next();
            ServerLevel level = server.getLevel(pendingEntry.getKey());
            DimensionEntry entry = level != null ? getEntryByDimension(server, pendingEntry.getKey()) : null;
            if (level == null || entry == null) {
                iterator.remove();
                RECARPET_TASKS.remove(pendingEntry.getKey());
                continue;
            }
            Queue<ChunkPos> pending = pendingEntry.getValue();
            RecarpetTask task = RECARPET_TASKS.computeIfAbsent(pendingEntry.getKey(),
                    key -> createRecarpetTask(entry, entry.floorLayout.clampedFloorCount(
                            PersonalDimensionConfig.COMMON.personalDimensionFloorY.get())));
            dispatchRecarpetCompute(pending, task);
            applyRecarpetResults(level, task);
            if (task.isFinished(pending)) {
                iterator.remove();
                RECARPET_TASKS.remove(pendingEntry.getKey());
                PersonalPlaneMod.LOGGER.info("[Pdim] Finished recarpet in {}", pendingEntry.getKey().location());
            }
        }
    }

    private static RecarpetTask createRecarpetTask(DimensionEntry entry, int clearUpToFloors) {
        int floorY = PersonalDimensionConfig.COMMON.personalDimensionFloorY.get();
        int floors = entry.floorLayout.clampedFloorCount(floorY);
        IFloorPreset preset = ModFloorStyles.applyLayout(PresetLoader.getPreset(), entry.floorLayout);
        return new RecarpetTask(preset, entry.colorScheme.copy(), floorY, floors,
                Mth.clamp(clearUpToFloors, floors, PersonalDimFloorLayout.maxFloorCount(floorY)));
    }

    private static void dispatchRecarpetCompute(Queue<ChunkPos> pending, RecarpetTask task) {
        while (!pending.isEmpty() && task.inFlight.get() + task.computed.size() < RECARPET_COMPUTE_AHEAD) {
            ChunkPos chunkPos = pending.poll();
            if (chunkPos == null) {
                break;
            }
            task.inFlight.incrementAndGet();
            RECARPET_EXECUTOR.execute(() -> {
                try {
                    task.computed.add(new ComputedChunk(chunkPos,
                            computeRecarpetStates(chunkPos, task.preset, task.scheme)));
                } catch (Exception e) {
                    PersonalPlaneMod.LOGGER.warn("[Pdim] Failed to precompute recarpet chunk {}", chunkPos, e);
                } finally {
                    task.inFlight.decrementAndGet();
                }
            });
        }
    }

    public static BlockState[] computeRecarpetStates(ChunkPos chunkPos, IFloorPreset preset,
            FloorColorScheme scheme) {
        BlockState[] desired = new BlockState[256];
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                BlockState state = preset.getState(chunkPos.getBlockX(lx), chunkPos.getBlockZ(lz));
                if (state == null) {
                    state = Blocks.AIR.defaultBlockState();
                }
                desired[lz * 16 + lx] = scheme.apply(state);
            }
        }
        return desired;
    }

    private static void applyRecarpetResults(ServerLevel level, RecarpetTask task) {
        for (int i = 0; i < RECARPET_CHUNKS_PER_TICK; i++) {
            ComputedChunk computed = task.computed.poll();
            if (computed == null) {
                break;
            }
            applyRecarpetChunk(level, computed.pos(), computed.desired(), task.floorY, task.floors,
                    task.clearUpTo);
        }
    }

    public static void applyRecarpetChunk(ServerLevel level, ChunkPos chunkPos, BlockState[] desired, int floorY,
            int floors, int clearUpTo) {
        LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
        boolean changed = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int layer = 0; layer < Math.max(floors, clearUpTo); layer++) {
            int layerY = PersonalDimFloorLayout.layerY(floorY, layer);
            boolean active = layer < floors;
            for (int index = 0; index < 256; index++) {
                BlockState state = desired[index];
                pos.set(chunkPos.getBlockX(index & 15), layerY, chunkPos.getBlockZ(index >> 4));
                if (active) {
                    if (!chunk.getBlockState(pos).equals(state)) {
                        chunk.setBlockState(pos, state, false);
                        changed = true;
                    }
                } else if (!state.isAir() && chunk.getBlockState(pos).equals(state)) {
                    chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                    changed = true;
                }
            }
        }
        if (!changed) {
            return;
        }
        chunk.setUnsaved(true);
        if (level.players().isEmpty()) {
            return;
        }
        ClientboundLevelChunkWithLightPacket packet = new ClientboundLevelChunkWithLightPacket(chunk,
                level.getLightEngine(), null, null);
        for (ServerPlayer viewer : level.players()) {
            viewer.connection.send(packet);
        }
    }

    private static List<ChunkPos> listGeneratedChunks(Path regionDir) {
        List<ChunkPos> result = new ArrayList<>();
        if (!Files.isDirectory(regionDir)) {
            return result;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(regionDir, "r.*.mca")) {
            for (Path file : stream) {
                String[] parts = file.getFileName().toString().split("\\.");
                if (parts.length != 4) {
                    continue;
                }
                int regionX = Integer.parseInt(parts[1]);
                int regionZ = Integer.parseInt(parts[2]);
                byte[] header = new byte[4096];
                try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
                    if (raf.length() < header.length) {
                        continue;
                    }
                    raf.readFully(header);
                }
                for (int i = 0; i < 1024; i++) {
                    int offset = ((header[i * 4] & 0xFF) << 16) | ((header[i * 4 + 1] & 0xFF) << 8)
                            | (header[i * 4 + 2] & 0xFF);
                    int sectors = header[i * 4 + 3] & 0xFF;
                    if (offset != 0 && sectors != 0) {
                        result.add(new ChunkPos(regionX * 32 + (i & 31), regionZ * 32 + (i >> 5)));
                    }
                }
            }
        } catch (IOException | NumberFormatException e) {
            PersonalPlaneMod.LOGGER.warn("[Pdim] Failed to scan region directory {}", regionDir, e);
        }
        return result;
    }

    // ==================== 删除 ====================

    public static boolean deleteDimension(MinecraftServer server, OwnerKey owner) {
        DimensionEntry entry = PersonalDimensionData.get(server).peekDimensionEntry(owner);
        if (entry == null || !entry.created) {
            return false;
        }
        ResourceKey<Level> key = dimensionKeyFor(owner);
        ServerLevel level = server.getLevel(key);
        if (level != null) {
            ServerLevel overworld = server.overworld();
            BlockPos spawn = overworld.getSharedSpawnPos();
            for (ServerPlayer player : new ArrayList<>(level.players())) {
                if (owner.isPlayer() && player.getUUID().equals(owner.id())) {
                    teleportToReturnPoint(player);
                } else {
                    player.teleportTo(overworld, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                            0.0f, 0.0f);
                }
                PlayerAbilityApplier.resetAbilities(player);
            }
            try {
                level.save(null, true, false);
                level.close();
            } catch (Exception e) {
                PersonalPlaneMod.LOGGER.warn("[Pdim] Failed to save/close dimension {}", key.location(), e);
            }
            BorderChangeListener listener = BORDER_LISTENERS.remove(key);
            if (listener != null) {
                server.overworld().getWorldBorder().removeListener(listener);
            }
            MinecraftForge.EVENT_BUS.post(new LevelEvent.Unload(level));
            ((MinecraftServerAccessor) server).getLevels().remove(key);
            server.markWorldsDirty();
        }
        PENDING_RECARPET.remove(key);
        RECARPET_TASKS.remove(key);
        RECARPET_COOLDOWN.remove(owner);
        PersonalDimensionData.get(server).removeDimensionEntry(owner);
        return true;
    }

    // ==================== 事件：规则 ====================

    @SubscribeEvent
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        Level level = event.getLevel().getLevel();
        if (!isPersonalDimension(level.dimension())) {
            return;
        }
        if (event.getEntity() instanceof Phantom) {
            event.setSpawnCancelled(true);
            return;
        }
        MinecraftServer server = event.getEntity().getServer();
        if (server == null) {
            return;
        }
        DimensionEntry entry = getEntryByDimension(server, level.dimension());
        if (entry != null && entry.rules.disableMobSpawning) {
            event.setSpawnCancelled(true);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }
        if (!isPersonalDimension(level.dimension()) || level.players().isEmpty()) {
            return;
        }
        DimensionEntry entry = getEntryByDimension(level.getServer(), level.dimension());
        if (entry == null) {
            return;
        }
        PersonalDimensionRules rules = entry.rules;
        if (rules.lockWeather && (level.isRaining() || level.isThundering())) {
            level.setWeatherParameters(6000, 0, false, false);
        }
        if (rules.lockTime || !rules.daylightCycle) {
            level.setDayTime(rules.timeValue);
        }
    }

    private static final Set<UUID> DIED_IN_PERSONAL_DIMENSION = ConcurrentHashMap.newKeySet();

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (isPersonalDimension(player.level().dimension())) {
            DIED_IN_PERSONAL_DIMENSION.add(player.getUUID());
        } else {
            DIED_IN_PERSONAL_DIMENSION.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (DIED_IN_PERSONAL_DIMENSION.remove(player.getUUID())) {
            applyDimensionRules(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        applyDimensionRules(player);
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (isPersonalDimension(event.getTo()) || isPersonalDimension(event.getFrom())) {
            applyDimensionRules(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        DimensionEntry entry = getEntryByDimension(player.server, player.level().dimension());
        PlayerAbilityApplier.reconcile(player, entry != null ? entry.rules : null);
        checkTeamAccess(player);
    }

    /**
     * 退队/被踢/队伍解散后，若玩家仍停留在已无 ENTER 权限的队伍维度内，安全送回返回点。
     */
    private static final Map<UUID, Long> TEAM_ACCESS_RECHECK = new ConcurrentHashMap<>();

    private static void checkTeamAccess(ServerPlayer player) {
        ResourceKey<Level> dimension = player.level().dimension();
        if (!isPersonalDimension(dimension)) {
            return;
        }
        OwnerKey owner = ownerFromKey(dimension);
        if (owner == null || !owner.isTeam()) {
            return;
        }
        if (AccessPolicies.current().can(player, owner, AccessAction.ENTER)) {
            return;
        }
        long now = player.level().getGameTime();
        Long last = TEAM_ACCESS_RECHECK.get(player.getUUID());
        if (last != null && now - last < 20L) {
            return;
        }
        TEAM_ACCESS_RECHECK.put(player.getUUID(), now);
        sendDenyMessage(player, "chat.personal_plane.no_access");
        teleportToReturnPoint(player);
    }

    // ==================== 事件：权限强制 ====================

    private static final Map<UUID, Long> DENY_MESSAGE_COOLDOWN = new ConcurrentHashMap<>();

    private static void sendDenyMessage(ServerPlayer player, String langKey) {
        long now = player.level().getGameTime();
        int cooldown = PersonalDimensionConfig.COMMON.denyMessageCooldownTicks.get();
        Long last = DENY_MESSAGE_COOLDOWN.get(player.getUUID());
        if (last != null && now - last < cooldown) {
            return;
        }
        DENY_MESSAGE_COOLDOWN.put(player.getUUID(), now);
        player.sendSystemMessage(Component.translatable(langKey), true);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        DENY_MESSAGE_COOLDOWN.remove(event.getEntity().getUUID());
        TEAM_ACCESS_RECHECK.remove(event.getEntity().getUUID());
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerAbilityApplier.resetAbilities(player);
        }
    }

    @SubscribeEvent
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        if (!hasPermission(player, player.level().dimension(), AccessAction.BUILD)) {
            event.setCanceled(true);
            sendDenyMessage(player, "chat.personal_plane.no_build");
        }
    }

    @SubscribeEvent
    public static void onPlaceBlock(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!hasPermission(player, player.level().dimension(), AccessAction.BUILD)) {
            event.setCanceled(true);
            sendDenyMessage(player, "chat.personal_plane.no_build");
        }
    }

    @SubscribeEvent
    public static void onFillBucket(FillBucketEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!hasPermission(player, player.level().dimension(), AccessAction.BUILD)) {
            event.setCanceled(true);
            sendDenyMessage(player, "chat.personal_plane.no_build");
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!hasPermission(player, event.getLevel().dimension(), AccessAction.INTERACT)) {
            event.setCanceled(true);
            sendDenyMessage(player, "chat.personal_plane.no_interact");
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!hasPermission(player, event.getLevel().dimension(), AccessAction.INTERACT)) {
            event.setCanceled(true);
            sendDenyMessage(player, "chat.personal_plane.no_interact");
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!hasPermission(player, player.level().dimension(), AccessAction.INTERACT)) {
            event.setCanceled(true);
            sendDenyMessage(player, "chat.personal_plane.no_interact");
        }
    }
}
