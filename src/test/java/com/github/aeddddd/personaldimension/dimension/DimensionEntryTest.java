package com.github.aeddddd.personaldimension.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DimensionEntryTest {

    @Test
    void nbtRoundTripPreservesDimensionData() {
        DimensionEntry entry = new DimensionEntry();
        entry.created = true;
        entry.rules.disableMobSpawning = true;
        entry.rules.flightEnabled = true;
        entry.rules.movementSpeed = 1.5f;
        entry.entryPoint = new BlockPos(12, 65, -4);
        entry.floorLayout.floorCount = 4;
        entry.floorLayout.roadEnabled = false;
        entry.floorLayout.roadChunks = 9;
        entry.hasStoredLevelData = true;
        entry.dayTime = 12000L;
        entry.raining = true;

        CompoundTag tag = entry.writeToNBT();
        DimensionEntry restored = new DimensionEntry();
        restored.readFromNBT(tag);

        assertThat(restored.created).isTrue();
        assertThat(restored.rules.disableMobSpawning).isTrue();
        assertThat(restored.rules.flightEnabled).isTrue();
        assertThat(restored.rules.movementSpeed).isEqualTo(1.5f);
        assertThat(restored.entryPoint).isEqualTo(new BlockPos(12, 65, -4));
        assertThat(restored.floorLayout.floorCount).isEqualTo(4);
        assertThat(restored.floorLayout.roadEnabled).isFalse();
        assertThat(restored.floorLayout.roadChunks).isEqualTo(9);
        assertThat(restored.hasStoredLevelData).isTrue();
        assertThat(restored.dayTime).isEqualTo(12000L);
        assertThat(restored.raining).isTrue();
    }

    @Test
    void missingOptionalKeysUseDefaults() {
        DimensionEntry entry = new DimensionEntry();
        entry.readFromNBT(new CompoundTag());

        assertThat(entry.created).isFalse();
        assertThat(entry.floorLayout.floorCount).isEqualTo(1);
        assertThat(entry.hasStoredLevelData).isFalse();
    }
}
