package com.github.aeddddd.personaldimension.dimension;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OwnerKeyTest {

    private static final UUID ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @Test
    void playerKeyRoundTrip() {
        OwnerKey key = OwnerKey.player(ID);
        assertThat(key.isPlayer()).isTrue();
        assertThat(key.isTeam()).isFalse();
        assertThat(key.serializedName()).isEqualTo("player:" + ID);
        assertThat(OwnerKey.parse(key.serializedName())).isEqualTo(key);
    }

    @Test
    void teamKeyRoundTrip() {
        OwnerKey key = OwnerKey.team(ID);
        assertThat(key.isTeam()).isTrue();
        assertThat(key.serializedName()).isEqualTo("team:" + ID);
        assertThat(OwnerKey.parse(key.serializedName())).isEqualTo(key);
    }

    @Test
    void parseRejectsMalformedValues() {
        assertThat(OwnerKey.parse(null)).isNull();
        assertThat(OwnerKey.parse("")).isNull();
        assertThat(OwnerKey.parse("player")).isNull();
        assertThat(OwnerKey.parse("nobody:" + ID)).isNull();
        assertThat(OwnerKey.parse("player:not-a-uuid")).isNull();
    }

    @Test
    void nbtRoundTrip() {
        OwnerKey key = OwnerKey.team(ID);
        assertThat(OwnerKey.readFromNBT(key.writeToNBT())).isEqualTo(key);
    }
}
