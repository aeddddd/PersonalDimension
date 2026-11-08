package com.github.aeddddd.personaldimension.dimension;

import com.github.aeddddd.personaldimension.access.AccessPolicies;
import com.github.aeddddd.personaldimension.access.FakeTeamAccess;
import com.github.aeddddd.personaldimension.access.TeamAccess;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OwnerResolverTest {

    private static final UUID PLAYER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TEAM = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @AfterEach
    void reset() {
        AccessPolicies.reset();
    }

    @Test
    void teamModeUsesPartyTeam() {
        FakeTeamAccess teams = new FakeTeamAccess();
        teams.addTeam(PLAYER, new TeamAccess.TeamRef(TEAM, "party", false));
        AccessPolicies.overrideTeamAccessForTest(teams);

        assertThat(OwnerResolver.resolve(PLAYER, DimensionMode.TEAM)).isEqualTo(OwnerKey.team(TEAM));
    }

    @Test
    void teamModeNormalizesPersonalTeamToPlayer() {
        FakeTeamAccess teams = new FakeTeamAccess();
        teams.addTeam(PLAYER, new TeamAccess.TeamRef(PLAYER, "solo", true));
        AccessPolicies.overrideTeamAccessForTest(teams);

        assertThat(OwnerResolver.resolve(PLAYER, DimensionMode.TEAM)).isEqualTo(OwnerKey.player(PLAYER));
    }

    @Test
    void personalModeAlwaysUsesPlayerDimension() {
        FakeTeamAccess teams = new FakeTeamAccess();
        teams.addTeam(PLAYER, new TeamAccess.TeamRef(TEAM, "party", false));
        AccessPolicies.overrideTeamAccessForTest(teams);

        assertThat(OwnerResolver.resolve(PLAYER, DimensionMode.PERSONAL)).isEqualTo(OwnerKey.player(PLAYER));
    }

    @Test
    void withoutFtbFallsBackToPlayerDimension() {
        AccessPolicies.reset();
        assertThat(OwnerResolver.resolve(PLAYER, DimensionMode.TEAM)).isEqualTo(OwnerKey.player(PLAYER));
    }
}
