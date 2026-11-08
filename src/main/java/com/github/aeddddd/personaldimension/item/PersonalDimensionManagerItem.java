package com.github.aeddddd.personaldimension.item;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * 个人维度管理器物品：作为合成剩余物保留，使「核心」配方不消耗管理器。
 */
public class PersonalDimensionManagerItem extends BlockItem {

    public PersonalDimensionManagerItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return new ItemStack(this);
    }
}
