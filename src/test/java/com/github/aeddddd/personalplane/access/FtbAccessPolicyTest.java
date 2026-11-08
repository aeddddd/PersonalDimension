package com.github.aeddddd.personalplane.access;

import com.github.aeddddd.personalplane.dimension.OwnerKey;
import com.github.aeddddd.personalplane.testutil.ForgeConfigTestBootstrap;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import com.github.aeddddd.personalplane.testutil.MinecraftTestBootstrap;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FtbAccessPolicyTest {

    @BeforeAll
    static void bootstrapVanilla() {
        MinecraftTestBootstrap.bootstrap();
    }


    private static final UUID VIEWER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID TEAM = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private FakeTeamAccess teams;
    private FtbAccessPolicy policy;
    private ServerPlayer player;

    @BeforeAll
    static void bootstrap() {
        ForgeConfigTestBootstrap.bootstrap();
    }

    @BeforeEach
    void setUp() {
        teams = new FakeTeamAccess();
        policy = new FtbAccessPolicy(teams);
        player = mock(ServerPlayer.class);
        when(player.getUUID()).thenReturn(VIEWER);
        when(player.getGameProfile()).thenReturn(new GameProfile(VIEWER, "viewer"));
        when(player.hasPermissions(anyInt())).thenReturn(false);
    }

    @Test
    void teamMembersCanEnterBuildAndInteract() {
        teams.setRank(TEAM, VIEWER, TeamAccess.Rank.MEMBER);
        OwnerKey owner = OwnerKey.team(TEAM);

        assertThat(policy.can(player, owner, AccessAction.ENTER)).isTrue();
        assertThat(policy.can(player, owner, AccessAction.BUILD)).isTrue();
        assertThat(policy.can(player, owner, AccessAction.INTERACT)).isTrue();
    }

    @Test
    void alliesCanEnterButNotBuildByDefault() {
        teams.setRank(TEAM, VIEWER, TeamAccess.Rank.ALLY);
        OwnerKey owner = OwnerKey.team(TEAM);

        assertThat(policy.can(player, owner, AccessAction.ENTER)).isTrue();
        assertThat(policy.can(player, owner, AccessAction.BUILD)).isFalse();
        assertThat(policy.can(player, owner, AccessAction.INTERACT)).isFalse();
        assertThat(policy.can(player, owner, AccessAction.MANAGE)).isFalse();
    }

    @Test
    void outsidersAreDenied() {
        teams.setRank(TEAM, VIEWER, TeamAccess.Rank.NONE);
        OwnerKey owner = OwnerKey.team(TEAM);

        assertThat(policy.can(player, owner, AccessAction.ENTER)).isFalse();
        assertThat(policy.can(player, owner, AccessAction.BUILD)).isFalse();
    }

    @Test
    void officersCanManageAndMembersCannot() {
        OwnerKey owner = OwnerKey.team(TEAM);

        teams.setRank(TEAM, VIEWER, TeamAccess.Rank.OFFICER);
        assertThat(policy.can(player, owner, AccessAction.MANAGE)).isTrue();

        teams.setRank(TEAM, VIEWER, TeamAccess.Rank.MEMBER);
        assertThat(policy.can(player, owner, AccessAction.MANAGE)).isFalse();
    }

    @Test
    void personalDimensionIsOwnerOnly() {
        OwnerKey own = OwnerKey.player(VIEWER);
        OwnerKey other = OwnerKey.player(OTHER);

        assertThat(policy.can(player, own, AccessAction.ENTER)).isTrue();
        assertThat(policy.can(player, own, AccessAction.BUILD)).isTrue();
        assertThat(policy.can(player, other, AccessAction.ENTER)).isFalse();
    }

    @Test
    void opOverrideBypassesTeamRank() {
        when(player.hasPermissions(2)).thenReturn(true);
        OwnerKey owner = OwnerKey.team(TEAM);
        assertThat(policy.can(player, owner, AccessAction.MANAGE)).isTrue();
    }
}
