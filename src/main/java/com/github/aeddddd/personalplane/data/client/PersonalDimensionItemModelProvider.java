package com.github.aeddddd.personalplane.data.client;

import com.github.aeddddd.personalplane.PersonalPlaneMod;
import com.github.aeddddd.personalplane.registry.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

/**
 * 物品模型数据生成器。
 */
public class PersonalDimensionItemModelProvider extends ItemModelProvider {

    public PersonalDimensionItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, PersonalPlaneMod.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(ModItems.PERSONAL_DIMENSION.get());
        withExistingParent("personal_plane_manager", block("personal_plane_manager_inventory"));
        withExistingParent("caution_block", block("caution_block"));
    }

    private ResourceLocation block(String name) {
        return new ResourceLocation(PersonalPlaneMod.MOD_ID, "block/" + name);
    }
}
