package com.github.aeddddd.personaldimension.dimension;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import com.github.aeddddd.personaldimension.testutil.MinecraftTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PersonalDimensionManagerKeyTest {

    @BeforeAll
    static void bootstrapVanilla() {
        MinecraftTestBootstrap.bootstrap();
    }


    private static final UUID ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final String HEX = "123e4567e89b12d3a456426614174000";

    @Test
    void playerDimensionKeyRoundTrip() {
        ResourceKey<Level> key = PersonalDimensionManager.dimensionKeyFor(OwnerKey.player(ID));
        assertThat(key.location()).isEqualTo(new ResourceLocation("personal_dimension", "pd_" + HEX));
        assertThat(PersonalDimensionManager.isPersonalDimension(key)).isTrue();
        assertThat(PersonalDimensionManager.ownerFromKey(key)).isEqualTo(OwnerKey.player(ID));
    }

    @Test
    void teamDimensionKeyRoundTrip() {
        ResourceKey<Level> key = PersonalDimensionManager.dimensionKeyFor(OwnerKey.team(ID));
        assertThat(key.location()).isEqualTo(new ResourceLocation("personal_dimension", "td_" + HEX));
        assertThat(PersonalDimensionManager.isPersonalDimension(key)).isTrue();
        assertThat(PersonalDimensionManager.ownerFromKey(key)).isEqualTo(OwnerKey.team(ID));
    }

    @Test
    void nonPersonalDimensionIsRejected() {
        ResourceKey<Level> overworld = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                new ResourceLocation("minecraft", "overworld"));
        assertThat(PersonalDimensionManager.isPersonalDimension(overworld)).isFalse();
        assertThat(PersonalDimensionManager.ownerFromKey(overworld)).isNull();
    }
}
