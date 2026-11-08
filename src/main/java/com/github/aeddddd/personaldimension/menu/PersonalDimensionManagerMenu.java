package com.github.aeddddd.personaldimension.menu;

import com.github.aeddddd.personaldimension.dimension.OwnerKey;
import com.github.aeddddd.personaldimension.dimension.PersonalDimensionManager;
import com.github.aeddddd.personaldimension.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * 个人维度管理器菜单：纯配置界面，无槽位。
 */
public class PersonalDimensionManagerMenu extends AbstractContainerMenu {

    public final BlockPos pos;
    public final OwnerKey owner;

    public PersonalDimensionManagerMenu(int id, Inventory inv, BlockPos pos, OwnerKey owner) {
        super(ModMenus.PERSONAL_DIMENSION_MANAGER.get(), id);
        this.pos = pos;
        this.owner = owner;
    }

    public static PersonalDimensionManagerMenu create(int id, Inventory inv, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int type = buf.readByte();
        OwnerKey owner = new OwnerKey(OwnerKey.Type.values()[Math.max(0,
                Math.min(type, OwnerKey.Type.values().length - 1))], buf.readUUID());
        return new PersonalDimensionManagerMenu(id, inv, pos, owner);
    }

    @Override
    public boolean stillValid(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            return PersonalDimensionManager.canManage(serverPlayer, owner);
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
