package com.github.aeddddd.personalplane.dimension;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PersonalDimFloorLayoutTest {

    @Test
    void defaultsShouldMatchLegacyBehavior() {
        PersonalDimFloorLayout layout = new PersonalDimFloorLayout();
        assertThat(layout.floorCount).isEqualTo(1);
        assertThat(layout.roadEnabled).isTrue();
        assertThat(layout.roadChunks).isEqualTo(PersonalDimFloorLayout.DEFAULT_ROAD_CHUNKS);
    }

    @Test
    void maxFloorCountShouldKeepTopLayerBelowLimit() {
        int floorY = 64;
        int max = PersonalDimFloorLayout.maxFloorCount(floorY);
        assertThat(max).isEqualTo(PersonalDimFloorLayout.MAX_FLOOR_COUNT);
        assertThat(PersonalDimFloorLayout.layerY(floorY, max - 1))
                .isLessThanOrEqualTo(PersonalDimFloorLayout.MAX_LAYER_Y);
        assertThat(PersonalDimFloorLayout.maxFloorCount(250)).isEqualTo(1);
        assertThat(PersonalDimFloorLayout.maxFloorCount(120)).isEqualTo(9);
    }

    @Test
    void layerYShouldAdvanceBySpacing() {
        assertThat(PersonalDimFloorLayout.layerY(64, 0)).isEqualTo(64);
        assertThat(PersonalDimFloorLayout.layerY(64, 1)).isEqualTo(64 + PersonalDimFloorLayout.LAYER_SPACING);
        assertThat(PersonalDimFloorLayout.layerY(64, 3)).isEqualTo(64 + 3 * PersonalDimFloorLayout.LAYER_SPACING);
    }

    @Test
    void clampShouldRespectFloorYAndBounds() {
        PersonalDimFloorLayout layout = new PersonalDimFloorLayout();
        layout.floorCount = 99;
        layout.roadChunks = 99;
        layout.clamp(64);
        assertThat(layout.floorCount).isEqualTo(PersonalDimFloorLayout.maxFloorCount(64));
        assertThat(layout.roadChunks).isEqualTo(PersonalDimFloorLayout.MAX_ROAD_CHUNKS);

        layout.floorCount = 0;
        layout.roadChunks = 0;
        layout.clamp(64);
        assertThat(layout.floorCount).isEqualTo(1);
        assertThat(layout.roadChunks).isEqualTo(PersonalDimFloorLayout.MIN_ROAD_CHUNKS);
    }

    @Test
    void nbtRoundTripShouldPreserveAllFields() {
        PersonalDimFloorLayout layout = new PersonalDimFloorLayout();
        layout.floorCount = 5;
        layout.roadEnabled = false;
        layout.roadChunks = 11;

        PersonalDimFloorLayout restored = new PersonalDimFloorLayout();
        restored.readFromNBT(layout.writeToNBT());

        assertThat(restored.floorCount).isEqualTo(5);
        assertThat(restored.roadEnabled).isFalse();
        assertThat(restored.roadChunks).isEqualTo(11);
    }

    @Test
    void readFromNbtShouldClampOutOfRangeValues() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("floorCount", 1000);
        tag.putBoolean("roadEnabled", true);
        tag.putInt("roadChunks", -3);

        PersonalDimFloorLayout layout = new PersonalDimFloorLayout();
        layout.readFromNBT(tag);

        assertThat(layout.floorCount).isEqualTo(PersonalDimFloorLayout.MAX_FLOOR_COUNT);
        assertThat(layout.roadChunks).isEqualTo(PersonalDimFloorLayout.MIN_ROAD_CHUNKS);
    }
}
