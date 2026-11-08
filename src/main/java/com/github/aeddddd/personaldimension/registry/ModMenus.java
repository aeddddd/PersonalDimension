package com.github.aeddddd.personaldimension.registry;

import com.github.aeddddd.personaldimension.PersonalDimensionMod;
import com.github.aeddddd.personaldimension.menu.PersonalDimensionCreateMenu;
import com.github.aeddddd.personaldimension.menu.PersonalDimensionManagerMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * 菜单类型注册中心。
 */
public final class ModMenus {

    public static final DeferredRegister<MenuType<?>> DR = DeferredRegister.create(Registries.MENU,
            PersonalDimensionMod.MOD_ID);

    public static final RegistryObject<MenuType<PersonalDimensionManagerMenu>> PERSONAL_DIMENSION_MANAGER = DR
            .register("personal_dimension_manager", () -> IForgeMenuType.create(PersonalDimensionManagerMenu::create));

    public static final RegistryObject<MenuType<PersonalDimensionCreateMenu>> PERSONAL_DIMENSION_CREATE = DR
            .register("personal_dimension_create", () -> IForgeMenuType.create(PersonalDimensionCreateMenu::create));

    private ModMenus() {
    }
}
