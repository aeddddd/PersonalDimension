package com.github.aeddddd.personaldimension.registry;

import com.github.aeddddd.personaldimension.PersonalDimensionMod;
import com.github.aeddddd.personaldimension.menu.PersonalDimensionCreateMenu;
import com.github.aeddddd.personaldimension.menu.PersonalDimensionManagerMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * 菜单类型注册中心。
 */
public final class ModMenus {

    public static final DeferredRegister<MenuType<?>> DR = DeferredRegister.create(Registries.MENU,
            PersonalDimensionMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<PersonalDimensionManagerMenu>> PERSONAL_DIMENSION_MANAGER = DR
            .register("personal_dimension_manager", () -> IMenuTypeExtension.create(PersonalDimensionManagerMenu::create));

    public static final DeferredHolder<MenuType<?>, MenuType<PersonalDimensionCreateMenu>> PERSONAL_DIMENSION_CREATE = DR
            .register("personal_dimension_create", () -> IMenuTypeExtension.create(PersonalDimensionCreateMenu::create));

    private ModMenus() {
    }
}
