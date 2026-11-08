package com.github.aeddddd.personaldimension.data.client;

import com.github.aeddddd.personaldimension.PersonalDimensionMod;
import com.github.aeddddd.personaldimension.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

/**
 * 方块状态与模型数据生成器。
 *
 * <p>管理器方块本体只生成扁平底座；上方终端由 BER 用隐藏终端方块的模型渲染。
 * 物品栏模型额外包含终端，避免手持/物品展示时只剩一个底座。</p>
 */
public class PersonalDimensionBlockStateProvider extends BlockStateProvider {

    public PersonalDimensionBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, PersonalDimensionMod.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        simpleBlock(ModBlocks.CAUTION_BLOCK.get());
        simpleBlock(ModBlocks.PERSONAL_DIMENSION_MANAGER.get(), managerBaseModel());
        simpleBlock(ModBlocks.PERSONAL_DIMENSION_TERMINAL.get(), terminalModel());
        managerInventoryModel();
    }

    private BlockModelBuilder managerBaseModel() {
        return models().getBuilder("block/personal_dimension_manager")
                .renderType("minecraft:cutout")
                .texture("particle", modLoc("block/personal_dimension_manager_base_side"))
                .texture("top", modLoc("block/personal_dimension_manager_base_top"))
                .texture("side", modLoc("block/personal_dimension_manager_base_side"))
                .texture("bottom", modLoc("block/personal_dimension_manager_base_bottom"))
                .element().from(1, 0, 1).to(15, 2, 15)
                .face(Direction.UP).texture("#top").end()
                .face(Direction.DOWN).texture("#bottom").end()
                .face(Direction.NORTH).texture("#side").end()
                .face(Direction.SOUTH).texture("#side").end()
                .face(Direction.EAST).texture("#side").end()
                .face(Direction.WEST).texture("#side").end()
                .end();
    }

    private BlockModelBuilder terminalModel() {
        return models().getBuilder("block/personal_dimension_terminal")
                .texture("particle", modLoc("block/personal_dimension_terminal_side"))
                .texture("side", modLoc("block/personal_dimension_terminal_side"))
                .texture("screen", modLoc("block/personal_dimension_terminal_screen"))
                .texture("cap", modLoc("block/personal_dimension_terminal_top"))
                .element().from(2, 0, 7).to(14, 11, 9)
                .face(Direction.NORTH).texture("#screen").end()
                .face(Direction.SOUTH).texture("#screen").end()
                .face(Direction.UP).texture("#cap").end()
                .face(Direction.DOWN).texture("#cap").end()
                .face(Direction.EAST).texture("#side").end()
                .face(Direction.WEST).texture("#side").end()
                .end();
    }

    private void managerInventoryModel() {
        models().getBuilder("block/personal_dimension_manager_inventory")
                .renderType("minecraft:cutout")
                .texture("particle", modLoc("block/personal_dimension_manager_base_side"))
                .texture("top", modLoc("block/personal_dimension_manager_base_top"))
                .texture("base_side", modLoc("block/personal_dimension_manager_base_side"))
                .texture("bottom", modLoc("block/personal_dimension_manager_base_bottom"))
                .texture("terminal_side", modLoc("block/personal_dimension_terminal_side"))
                .texture("screen", modLoc("block/personal_dimension_terminal_screen"))
                .texture("cap", modLoc("block/personal_dimension_terminal_top"))
                .element().from(1, 0, 1).to(15, 2, 15)
                .face(Direction.UP).texture("#top").end()
                .face(Direction.DOWN).texture("#bottom").end()
                .face(Direction.NORTH).texture("#base_side").end()
                .face(Direction.SOUTH).texture("#base_side").end()
                .face(Direction.EAST).texture("#base_side").end()
                .face(Direction.WEST).texture("#base_side").end()
                .end()
                .element().from(2, 2, 7).to(14, 13, 9)
                .face(Direction.NORTH).texture("#screen").end()
                .face(Direction.SOUTH).texture("#screen").end()
                .face(Direction.UP).texture("#cap").end()
                .face(Direction.DOWN).texture("#cap").end()
                .face(Direction.EAST).texture("#terminal_side").end()
                .face(Direction.WEST).texture("#terminal_side").end()
                .end();
    }
}
