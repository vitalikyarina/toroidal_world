package com.toroidalworld.compat.create;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class ChassisWalkFrame {
    private static final ThreadLocal<Scope> BOUND = new ThreadLocal<>();

    public static <T> T withAnchor(@Nullable Level level, BlockPos anchor, Supplier<T> body) {
        Scope previous = BOUND.get();
        BOUND.set(new Scope(level, anchor));
        try {
            return body.get();
        } finally {
            if (previous == null) {
                BOUND.remove();
            } else {
                BOUND.set(previous);
            }
        }
    }

    public static BlockPos fold(BlockPos worldPosition) {
        Scope scope = BOUND.get();
        if (scope == null) {
            return worldPosition;
        }

        return CreateSeamFold.foldPosition(scope.level, scope.anchor, worldPosition);
    }

    private record Scope(@Nullable Level level, BlockPos anchor) {
    }

    private ChassisWalkFrame() {
    }
}
