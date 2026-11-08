package com.github.aeddddd.personalplane.gametest;

import com.github.aeddddd.personalplane.PersonalDimensionConfig;
import com.github.aeddddd.personalplane.PersonalPlaneMod;
import com.github.aeddddd.personalplane.dimension.OwnerKey;
import com.github.aeddddd.personalplane.dimension.PersonalDimensionManager;
import com.github.aeddddd.personalplane.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.util.List;
import java.util.UUID;

/**
 * 个人维度动态创建的冒烟测试：验证运行时 ServerLevel 注入、区块生成与地板铺设（无基岩垫层）。
 */
@GameTestHolder(PersonalPlaneMod.MOD_ID)
public class PersonalDimensionGameTest {

    @GameTest(template = "empty")
    public static void personalDimensionCreation(GameTestHelper helper) {
        UUID owner = UUID.randomUUID();
        OwnerKey key = OwnerKey.player(owner);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel level = null;
        try {
            level = PersonalDimensionManager.getOrCreateDimension(server, key);
            helper.assertTrue(level != null, "个人维度创建失败");

            ResourceKey<Level> dimension = PersonalDimensionManager.dimensionKeyFor(key);
            helper.assertTrue(server.getLevel(dimension) == level, "个人维度未注入服务器维度表");
            helper.assertTrue(PersonalDimensionManager.isPersonalDimension(dimension), "维度键未被识别为个人维度");
            helper.assertTrue(key.equals(PersonalDimensionManager.ownerFromKey(dimension)), "无法从维度键反推归属");

            int floorY = PersonalDimensionConfig.COMMON.personalDimensionFloorY.get();
            BlockState floor = level.getBlockState(new BlockPos(1, floorY, 1));
            helper.assertTrue(!floor.isAir(), "个人维度地板未生成");
            helper.assertTrue(level.getBlockState(new BlockPos(1, floorY - 1, 1)).isAir(),
                    "地板下方不应生成基岩垫层");
            helper.assertTrue(level.getBlockState(new BlockPos(1, floorY + 1, 1)).isAir(),
                    "地板上方应为空气");
            helper.succeed();
        } catch (Throwable t) {
            PersonalPlaneMod.LOGGER.error("[Pdim] personalDimensionCreation test error", t);
            helper.fail(t.getMessage() != null ? t.getMessage() : t.toString());
        } finally {
            if (level != null) {
                PersonalDimensionManager.deleteDimension(server, key);
            }
        }
    }

    @GameTest(template = "empty")
    public static void managerRemainderKeepsManager(GameTestHelper helper) {
        Item manager = ModItems.PERSONAL_DIMENSION_MANAGER.get();
        Item core = ModItems.PERSONAL_DIMENSION.get();
        try {
            ItemStack stack = new ItemStack(manager);
            helper.assertTrue(stack.hasCraftingRemainingItem(), "管理器应作为合成剩余物保留");
            helper.assertTrue(stack.getCraftingRemainingItem().is(manager), "合成剩余物应为管理器本身");

            ShapelessRecipe coreRecipe = helper.getLevel().getRecipeManager()
                    .getAllRecipesFor(RecipeType.CRAFTING).stream()
                    .map(RecipeHolder::value)
                    .filter(recipe -> recipe instanceof ShapelessRecipe)
                    .map(recipe -> (ShapelessRecipe) recipe)
                    .filter(recipe -> recipe.getResultItem(helper.getLevel().registryAccess()).is(core))
                    .findFirst().orElse(null);
            helper.assertTrue(coreRecipe != null, "未找到核心的无序合成配方");

            CraftingInput input = CraftingInput.of(2, 1,
                    List.of(new ItemStack(manager), new ItemStack(Items.STICK)));
            helper.assertTrue(coreRecipe.matches(input, helper.getLevel()), "核心配方应匹配管理器与木棍");

            var remaining = coreRecipe.getRemainingItems(input);
            helper.assertTrue(remaining.get(0).is(manager), "合成后应保留管理器");
            helper.assertTrue(remaining.get(1).isEmpty(), "木棍应被消耗");
            helper.succeed();
        } catch (Throwable t) {
            PersonalPlaneMod.LOGGER.error("[Pdim] managerRemainderKeepsManager test error", t);
            helper.fail(t.getMessage() != null ? t.getMessage() : t.toString());
        }
    }
}
