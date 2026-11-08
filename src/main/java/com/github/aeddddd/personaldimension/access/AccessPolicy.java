package com.github.aeddddd.personaldimension.access;

import com.github.aeddddd.personaldimension.dimension.OwnerKey;
import net.minecraft.server.level.ServerPlayer;

/**
 * 个人维度访问策略。
 */
public interface AccessPolicy {

    boolean can(ServerPlayer player, OwnerKey owner, AccessAction action);
}
