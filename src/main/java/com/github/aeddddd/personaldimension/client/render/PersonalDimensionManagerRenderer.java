package com.github.aeddddd.personaldimension.client.render;

import com.github.aeddddd.personaldimension.blockentity.PersonalDimensionManagerBlockEntity;
import com.github.aeddddd.personaldimension.registry.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

/**
 * 个人维度终端渲染：在薄圆角底座上方渲染悬浮、自由朝向玩家的终端面板。
 *
 * <p>终端不是只绕 Y 轴转：以面板中心为轴同时计算 yaw/pitch，玩家从高处或低处看时
 * 面板都会转向玩家；无人时绕三轴缓慢自转。面板本身是隐藏方块的 blockstate/model。</p>
 */
public class PersonalDimensionManagerRenderer implements BlockEntityRenderer<PersonalDimensionManagerBlockEntity> {

    private static final double BASE_HEIGHT = 2.0 / 16.0;
    private static final double TERMINAL_CENTER_Y = 5.5 / 16.0;
    private static final double BOB_AMPLITUDE = 0.018;
    private static final double FOLLOW_RANGE = 20.0;
    private static final float TURN_LERP = 0.18f;

    public PersonalDimensionManagerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(PersonalDimensionManagerBlockEntity blockEntity, float partialTick, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockState terminal = ModBlocks.PERSONAL_DIMENSION_TERMINAL.get().defaultBlockState();
        double centerX = blockEntity.getBlockPos().getX() + 0.5;
        double centerY = blockEntity.getBlockPos().getY() + BASE_HEIGHT + TERMINAL_CENTER_Y;
        double centerZ = blockEntity.getBlockPos().getZ() + 0.5;
        double bob = 0.0;
        float targetYaw = blockEntity.renderYaw;
        float targetPitch = blockEntity.renderPitch;
        float targetRoll = blockEntity.renderRoll;
        if (blockEntity.getLevel() != null) {
            long time = blockEntity.getLevel().getGameTime();
            bob = Math.sin((time + partialTick) * 0.06) * BOB_AMPLITUDE;
            Player nearest = blockEntity.getLevel().getNearestPlayer(
                    centerX, centerY, centerZ, FOLLOW_RANGE, false);
            if (nearest != null) {
                double dx = nearest.getX() - centerX;
                double dy = nearest.getEyeY() - centerY;
                double dz = nearest.getZ() - centerZ;
                double horizontal = Math.sqrt(dx * dx + dz * dz);
                targetYaw = (float) Math.toDegrees(Math.atan2(dx, dz));
                targetPitch = Mth.clamp((float) -Math.toDegrees(Math.atan2(dy, horizontal)), -80.0f, 80.0f);
                // 极轻微的侧倾，让悬浮终端不显得完全刚性
                targetRoll = Mth.sin((time + partialTick) * 0.05f) * 2.5f;
            } else {
                // 无人时绕三轴缓慢自转
                targetYaw = (time + partialTick) * 1.5f;
                targetPitch = Mth.sin((time + partialTick) * 0.04f) * 25.0f;
                targetRoll = Mth.cos((time + partialTick) * 0.03f) * 15.0f;
            }
        }
        blockEntity.renderYaw = Mth.rotLerp(TURN_LERP, blockEntity.renderYaw, targetYaw);
        blockEntity.renderPitch = Mth.lerp(TURN_LERP, blockEntity.renderPitch, targetPitch);
        blockEntity.renderRoll = Mth.lerp(TURN_LERP, blockEntity.renderRoll, targetRoll);

        poseStack.pushPose();
        poseStack.translate(0.5, BASE_HEIGHT + TERMINAL_CENTER_Y + bob, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(blockEntity.renderYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(blockEntity.renderPitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(blockEntity.renderRoll));
        poseStack.translate(-0.5, -TERMINAL_CENTER_Y, -0.5);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(terminal, poseStack, buffer,
                packedLight, packedOverlay, ModelData.EMPTY, RenderType.cutout());
        poseStack.popPose();
    }
}
