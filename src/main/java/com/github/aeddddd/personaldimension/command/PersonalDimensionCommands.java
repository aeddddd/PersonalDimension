package com.github.aeddddd.personaldimension.command;

import com.github.aeddddd.personaldimension.PersonalDimensionMod;
import com.github.aeddddd.personaldimension.dimension.DimensionEntry;
import com.github.aeddddd.personaldimension.dimension.DimensionMode;
import com.github.aeddddd.personaldimension.dimension.OwnerKey;
import com.github.aeddddd.personaldimension.dimension.OwnerResolver;
import com.github.aeddddd.personaldimension.dimension.PersonalDimensionData;
import com.github.aeddddd.personaldimension.dimension.PersonalDimensionManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

/**
 * {@code /pdim} 命令族。
 *
 * <p>list / info / delete 需要 OP≥2；tp 走 ENTER 权限；mode 切换自己的维度模式。</p>
 */
@Mod.EventBusSubscriber(modid = PersonalDimensionMod.MOD_ID)
public final class PersonalDimensionCommands {

    private PersonalDimensionCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("pdim")
                .then(Commands.literal("list")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> list(ctx.getSource())))
                .then(Commands.literal("info")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> info(ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("delete")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> delete(ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("tp")
                        .executes(ctx -> tpSelf(ctx.getSource()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> tpTo(ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("mode")
                        .then(Commands.literal("team")
                                .executes(ctx -> setMode(ctx.getSource(), DimensionMode.TEAM)))
                        .then(Commands.literal("personal")
                                .executes(ctx -> setMode(ctx.getSource(), DimensionMode.PERSONAL)))));
    }

    private static int list(CommandSourceStack source) {
        PersonalDimensionData data = PersonalDimensionData.get(source.getServer());
        int count = 0;
        for (Map.Entry<OwnerKey, DimensionEntry> entry : data.getAllDimensions()) {
            String name = OwnerResolver.displayName(source.getServer(), entry.getKey());
            source.sendSuccess(() -> Component.translatable(
                    "chat.personal_dimension.cmd.list_entry",
                    entry.getKey().type().name().toLowerCase() + ":" + name, entry.getValue().created), false);
            count++;
        }
        int total = count;
        source.sendSuccess(() -> Component.translatable(
                "chat.personal_dimension.cmd.list_total", total), false);
        return total;
    }

    private static int info(CommandSourceStack source, ServerPlayer target) {
        OwnerKey owner = PersonalDimensionManager.resolveOwner(target.server, target.getUUID());
        DimensionEntry entry = PersonalDimensionData.get(source.getServer()).peekDimensionEntry(owner);
        if (entry == null || !entry.created) {
            source.sendFailure(Component.translatable(
                    "chat.personal_dimension.cmd.no_dimension", target.getName().getString()));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable(
                "chat.personal_dimension.cmd.info_header", OwnerResolver.displayName(source.getServer(), owner)),
                false);
        source.sendSuccess(() -> Component.translatable(
                "chat.personal_dimension.cmd.info_type",
                owner.isTeam() ? "team" : "personal"), false);
        source.sendSuccess(() -> Component.translatable(
                "chat.personal_dimension.cmd.info_entry",
                entry.entryPoint.getX(), entry.entryPoint.getY(), entry.entryPoint.getZ()), false);
        source.sendSuccess(() -> Component.translatable(
                "chat.personal_dimension.cmd.info_mode",
                PersonalDimensionManager.getMode(source.getServer(), target.getUUID()).name()), false);
        if (owner.isTeam()) {
            source.sendSuccess(() -> Component.translatable(
                    "chat.personal_dimension.cmd.info_members",
                    OwnerResolver.memberCount(owner)), false);
        }
        return 1;
    }

    private static int delete(CommandSourceStack source, ServerPlayer target) {
        OwnerKey owner = PersonalDimensionManager.resolveOwner(target.server, target.getUUID());
        if (PersonalDimensionManager.deleteDimension(source.getServer(), owner)) {
            source.sendSuccess(() -> Component.translatable(
                    "chat.personal_dimension.cmd.deleted",
                    OwnerResolver.displayName(source.getServer(), owner)), true);
            source.sendSuccess(() -> Component.translatable(
                    "chat.personal_dimension.cmd.deleted_files_kept"), false);
            return 1;
        }
        source.sendFailure(Component.translatable(
                "chat.personal_dimension.cmd.no_dimension", target.getName().getString()));
        return 0;
    }

    private static int tpSelf(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("chat.personal_dimension.cmd.player_only"));
            return 0;
        }
        OwnerKey owner = PersonalDimensionManager.resolveOwner(player);
        if (PersonalDimensionManager.teleportPlayerToDimension(player, owner)) {
            return 1;
        }
        source.sendFailure(Component.translatable("chat.personal_dimension.create_failed"));
        return 0;
    }

    private static int tpTo(CommandSourceStack source, ServerPlayer target) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("chat.personal_dimension.cmd.player_only"));
            return 0;
        }
        OwnerKey owner = PersonalDimensionManager.resolveOwner(target.server, target.getUUID());
        if (PersonalDimensionManager.teleportPlayerToDimension(player, owner)) {
            return 1;
        }
        source.sendFailure(Component.translatable(
                "chat.personal_dimension.cmd.no_permission", target.getName().getString()));
        return 0;
    }

    private static int setMode(CommandSourceStack source, DimensionMode mode) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("chat.personal_dimension.cmd.player_only"));
            return 0;
        }
        PersonalDimensionManager.setMode(player.server, player.getUUID(), mode);
        source.sendSuccess(() -> Component.translatable(mode == DimensionMode.TEAM
                ? "chat.personal_dimension.mode_team"
                : "chat.personal_dimension.mode_personal"), true);
        return 1;
    }
}
