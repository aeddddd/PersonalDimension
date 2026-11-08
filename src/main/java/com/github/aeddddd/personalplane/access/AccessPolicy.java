package com.github.aeddddd.personalplane.access;

import com.github.aeddddd.personalplane.dimension.OwnerKey;
import net.minecraft.server.level.ServerPlayer;

/**
 * 个人维度访问策略。
 */
public interface AccessPolicy {

    boolean can(ServerPlayer player, OwnerKey owner, AccessAction action);
}
