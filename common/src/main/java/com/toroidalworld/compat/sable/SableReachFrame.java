package com.toroidalworld.compat.sable;

import java.util.function.Supplier;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public final class SableReachFrame {
    public static boolean blockReach(Player player, BlockPos pos, double buffer, Operation<Boolean> original) {
        return framed(player, () -> original.call(pos, buffer));
    }

    public static boolean entityReach(Player player, AABB box, double buffer, Operation<Boolean> original) {
        return framed(player, () -> original.call(box, buffer));
    }

    public static boolean bedReach(ServerPlayer player, BlockPos pos, Operation<Boolean> original) {
        return framed(player, () -> original.call(pos));
    }

    private static boolean framed(Player player, Supplier<Boolean> body) {
        return SeamFrame.with(player.level(), player::position, body);
    }

    private SableReachFrame() {
    }
}
