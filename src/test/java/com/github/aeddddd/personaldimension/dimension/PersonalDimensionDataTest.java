package com.github.aeddddd.personaldimension.dimension;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PersonalDimensionDataTest {

    private static final UUID PLAYER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TEAM = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void saveAndLoadPreserveDimensionsAndPlayers() {
        PersonalDimensionData data = new PersonalDimensionData();
        OwnerKey playerOwner = OwnerKey.player(PLAYER);
        OwnerKey teamOwner = OwnerKey.team(TEAM);

        DimensionEntry playerEntry = data.getDimensionEntry(playerOwner);
        playerEntry.created = true;
        playerEntry.rules.flightEnabled = true;
        playerEntry.floorLayout.floorCount = 3;

        DimensionEntry teamEntry = data.getDimensionEntry(teamOwner);
        teamEntry.created = true;
        teamEntry.rules.disableMobSpawning = true;

        PlayerDimState state = data.getPlayerState(PLAYER);
        state.mode = DimensionMode.PERSONAL;
        state.hasReturnPoint = true;
        state.returnX = 3.0;

        CompoundTag tag = new CompoundTag();
        data.save(tag);
        PersonalDimensionData restored = PersonalDimensionData.load(tag);

        assertThat(restored.getDimensionEntry(playerOwner).created).isTrue();
        assertThat(restored.getDimensionEntry(playerOwner).rules.flightEnabled).isTrue();
        assertThat(restored.getDimensionEntry(playerOwner).floorLayout.floorCount).isEqualTo(3);
        assertThat(restored.getDimensionEntry(teamOwner).rules.disableMobSpawning).isTrue();
        assertThat(restored.getPlayerState(PLAYER).mode).isEqualTo(DimensionMode.PERSONAL);
        assertThat(restored.getPlayerState(PLAYER).hasReturnPoint).isTrue();
        assertThat(restored.getPlayerState(PLAYER).returnX).isEqualTo(3.0);
    }

    @Test
    void createdFlagTracksAnyCreatedDimension() {
        PersonalDimensionData data = new PersonalDimensionData();
        assertThat(data.hasAnyCreatedDimension()).isFalse();

        data.getDimensionEntry(OwnerKey.team(TEAM)).created = true;
        assertThat(data.hasAnyCreatedDimension()).isTrue();

        data.removeDimensionEntry(OwnerKey.team(TEAM));
        assertThat(data.hasAnyCreatedDimension()).isFalse();
    }
}
