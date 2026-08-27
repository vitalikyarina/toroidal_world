package com.toroidalworld.net;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.toroidalworld.core.WorldFold;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class ListenerCopies {
    public static List<Vec3> nearestTo(WorldFold transformer, Iterable<ServerPlayer> players, Predicate<ServerPlayer> listening,
            Vec3 source) {
        List<Vec3> copies = new ArrayList<>(1);
        for (ServerPlayer player : players) {
            if (!listening.test(player)) {
                continue;
            }

            Vec3 copy = transformer.nearestCopy(player.position(), source);
            if (!copies.contains(copy)) {
                copies.add(copy);
            }
        }

        return copies.isEmpty() ? List.of(source) : copies;
    }

    public static List<BlockPos> nearestTo(WorldFold transformer, Iterable<ServerPlayer> players, Predicate<ServerPlayer> listening,
            BlockPos source) {
        List<BlockPos> copies = new ArrayList<>(1);
        for (ServerPlayer player : players) {
            if (!listening.test(player)) {
                continue;
            }

            BlockPos copy = transformer.nearestCopy(player.blockPosition(), source);
            if (!copies.contains(copy)) {
                copies.add(copy);
            }
        }

        return copies.isEmpty() ? List.of(source) : copies;
    }

    private ListenerCopies() {
    }
}
