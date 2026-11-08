package com.github.aeddddd.personalplane.dimension;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlayerDimStateTest {

    @Test
    void nbtRoundTripPreservesModeAndReturnPoint() {
        PlayerDimState state = new PlayerDimState();
        state.mode = DimensionMode.PERSONAL;
        state.returnDim = "minecraft:the_nether";
        state.returnX = 1.5;
        state.returnY = 64.0;
        state.returnZ = -8.5;
        state.returnYaw = 90.0f;
        state.returnPitch = -10.0f;
        state.hasReturnPoint = true;

        CompoundTag tag = state.writeToNBT();
        PlayerDimState restored = new PlayerDimState();
        restored.readFromNBT(tag);

        assertThat(restored.mode).isEqualTo(DimensionMode.PERSONAL);
        assertThat(restored.returnDim).isEqualTo("minecraft:the_nether");
        assertThat(restored.returnX).isEqualTo(1.5);
        assertThat(restored.returnY).isEqualTo(64.0);
        assertThat(restored.returnZ).isEqualTo(-8.5);
        assertThat(restored.returnYaw).isEqualTo(90.0f);
        assertThat(restored.returnPitch).isEqualTo(-10.0f);
        assertThat(restored.hasReturnPoint).isTrue();
    }

    @Test
    void invalidModeFallsBackToTeam() {
        CompoundTag tag = new CompoundTag();
        tag.putString("mode", "SOMETHING_ELSE");
        PlayerDimState state = new PlayerDimState();
        state.readFromNBT(tag);
        assertThat(state.mode).isEqualTo(DimensionMode.TEAM);
        assertThat(state.returnDim).isEqualTo("minecraft:overworld");
    }
}
