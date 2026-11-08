package com.github.aeddddd.personaldimension.item;

import com.github.aeddddd.personaldimension.access.AccessAction;
import com.github.aeddddd.personaldimension.dimension.DimensionEntry;
import com.github.aeddddd.personaldimension.dimension.OwnerKey;
import com.github.aeddddd.personaldimension.dimension.OwnerResolver;
import com.github.aeddddd.personaldimension.dimension.PersonalDimensionManager;
import com.github.aeddddd.personaldimension.menu.PersonalDimensionCreateMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * 个人维度核心：按玩家当前模式进入个人维度或队伍共享维度。
 */
public class PersonalDimensionCoreItem extends Item {

    public PersonalDimensionCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            return bindEntryPoint(context);
        }
        toggleTeleport((ServerPlayer) player);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            toggleTeleport(serverPlayer);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    private InteractionResult bindEntryPoint(UseOnContext context) {
        ServerPlayer player = (ServerPlayer) context.getPlayer();
        OwnerKey owner = PersonalDimensionManager.resolveOwner(player);
        ResourceKey<Level> key = PersonalDimensionManager.dimensionKeyFor(owner);
        if (!context.getLevel().dimension().equals(key)) {
            player.sendSystemMessage(
                    Component.translatable("chat.personal_dimension.bind_only_in_dim"));
            return InteractionResult.FAIL;
        }
        if (!PersonalDimensionManager.hasPermission(player, key, AccessAction.BUILD)) {
            player.sendSystemMessage(Component.translatable("chat.personal_dimension.no_build"));
            return InteractionResult.FAIL;
        }
        PersonalDimensionManager.setEntryPoint(player.server, owner, context.getClickedPos().above());
        player.sendSystemMessage(Component.translatable("chat.personal_dimension.bound",
                context.getClickedPos().getX(), context.getClickedPos().getY() + 1,
                context.getClickedPos().getZ()));
        return InteractionResult.SUCCESS;
    }

    private static void toggleTeleport(ServerPlayer player) {
        OwnerKey owner = PersonalDimensionManager.resolveOwner(player);
        ResourceKey<Level> key = PersonalDimensionManager.dimensionKeyFor(owner);
        if (player.level().dimension().equals(key)) {
            PersonalDimensionManager.teleportToReturnPoint(player);
            return;
        }
        DimensionEntry entry = PersonalDimensionManager.getDimensionEntry(player.server, owner);
        if (!entry.created) {
            player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new PersonalDimensionCreateMenu(id, inv, owner),
                    Component.translatable("gui.personal_dimension.pdim.create_title")),
                    buf -> PersonalDimensionCreateMenu.writePreviewData(buf, owner,
                            OwnerResolver.displayName(player.server, owner)));
            return;
        }
        PersonalDimensionManager.setReturnPoint(player);
        ServerLevel level = PersonalDimensionManager.getOrCreateDimension(player.server, owner);
        if (level == null) {
            player.sendSystemMessage(
                    Component.translatable("chat.personal_dimension.create_failed"));
            return;
        }
        PersonalDimensionManager.teleportToDimension(player, owner);
    }
}
