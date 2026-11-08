package com.github.aeddddd.personaldimension.data.client;

import com.github.aeddddd.personaldimension.PersonalDimensionMod;
import com.github.aeddddd.personaldimension.registry.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * 物品模型数据生成器。
 */
public class PersonalDimensionItemModelProvider extends ItemModelProvider {

    public PersonalDimensionItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, PersonalDimensionMod.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(ModItems.PERSONAL_DIMENSION.get());
        withExistingParent("personal_dimension_manager", block("personal_dimension_manager_inventory"));
        withExistingParent("caution_block", block("caution_block"));
    }

    private ResourceLocation block(String name) {
        return ResourceLocation.fromNamespaceAndPath(PersonalDimensionMod.MOD_ID, "block/" + name);
    }
}
