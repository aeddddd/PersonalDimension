package com.github.aeddddd.personalplane.access;

import com.github.aeddddd.personalplane.dimension.OwnerKey;
import com.github.aeddddd.personalplane.testutil.ForgeConfigTestBootstrap;
import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.BeforeAll;
import com.github.aeddddd.personalplane.testutil.MinecraftTestBootstrap;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FallbackAccessPolicyTest {

    @BeforeAll
    static void bootstrapVanilla() {
        MinecraftTestBootstrap.bootstrap();
    }


    private static final UUID PLAYER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @BeforeAll
    static void bootstrap() {
        ForgeConfigTestBootstrap.bootstrap();
    }

    @Test
    void ownerCanDoEverythingAndOthersCannot() {
        ServerPlayer player = mock(ServerPlayer.class);
        when(player.getUUID()).thenReturn(PLAYER);
        when(player.hasPermissions(anyInt())).thenReturn(false);

        assertThat(FallbackAccessPolicy.INSTANCE.can(player, OwnerKey.player(PLAYER), AccessAction.BUILD))
                .isTrue();
        assertThat(FallbackAccessPolicy.INSTANCE.can(player, OwnerKey.player(OTHER), AccessAction.ENTER))
                .isFalse();
    }

    @Test
    void opCanBypass() {
        ServerPlayer player = mock(ServerPlayer.class);
        when(player.getUUID()).thenReturn(PLAYER);
        when(player.hasPermissions(2)).thenReturn(true);

        assertThat(FallbackAccessPolicy.INSTANCE.can(player, OwnerKey.player(OTHER), AccessAction.MANAGE))
                .isTrue();
    }
}
