package com.github.aeddddd.personalplane.block;

import com.github.aeddddd.personalplane.blockentity.PersonalDimensionManagerBlockEntity;
import com.github.aeddddd.personalplane.dimension.OwnerKey;
import com.github.aeddddd.personalplane.dimension.PersonalDimensionManager;
import com.github.aeddddd.personalplane.menu.PersonalDimensionManagerMenu;
import com.github.aeddddd.personalplane.network.ModNetwork;
import com.github.aeddddd.personalplane.network.packet.PersonalDimManagerStatePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkHooks;

/**
 * 个人维度管理器：无状态「维度终端」。
 *
 * <p>打开时按使用者当前维度模式解析归属（队伍或个人），因此切换模式后同一个方块仍可用；
 * 不再绑定放置者。需要 MANAGE 权限才能打开。</p>
 */
public class PersonalDimensionManagerBlock extends Block implements EntityBlock {

    private static final VoxelShape BASE_SHAPE = Shapes.box(
            1.0 / 16.0, 0.0, 1.0 / 16.0, 15.0 / 16.0, 2.0 / 16.0, 15.0 / 16.0);
    private static final VoxelShape TERMINAL_SHAPE = Shapes.box(
            2.0 / 16.0, 2.0 / 16.0, 7.0 / 16.0, 14.0 / 16.0, 13.0 / 16.0, 9.0 / 16.0);
    private static final VoxelShape SHAPE = Shapes.or(BASE_SHAPE, TERMINAL_SHAPE);

    public PersonalDimensionManagerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PersonalDimensionManagerBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return BASE_SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        OwnerKey owner = PersonalDimensionManager.resolveOwner(serverPlayer);
        if (!PersonalDimensionManager.canManage(serverPlayer, owner)) {
            serverPlayer.sendSystemMessage(
                    Component.translatable("chat.personal_plane.no_manage"));
            return InteractionResult.FAIL;
        }
        NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                (id, inv, p) -> new PersonalDimensionManagerMenu(id, inv, pos, owner),
                Component.translatable("gui.personal_plane.personal_plane_manager")),
                buf -> {
                    buf.writeBlockPos(pos);
                    buf.writeByte(owner.type().ordinal());
                    buf.writeUUID(owner.id());
                });
        ModNetwork.CHANNEL.sendTo(
                PersonalDimManagerStatePacket.create(serverPlayer.server, pos, owner, serverPlayer),
                serverPlayer.connection.connection,
                NetworkDirection.PLAY_TO_CLIENT);
        return InteractionResult.SUCCESS;
    }
}
