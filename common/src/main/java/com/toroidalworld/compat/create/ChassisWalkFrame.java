package com.toroidalworld.compat.create;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class ChassisWalkFrame {
    private static final ThreadScope<Scope> BOUND = new ThreadScope<>();

    public static <T> T withAnchor(@Nullable Level level, BlockPos anchor, Supplier<T> body) {
        return BOUND.with(new Scope(level, anchor), body);
    }

    public static BlockPos fold(BlockPos worldPosition) {
        Scope scope = BOUND.current();
        if (scope == null) {
            return worldPosition;
        }

        return CreateSeamFold.nearestCopy(scope.level, scope.anchor, worldPosition);
    }

    private record Scope(@Nullable Level level, BlockPos anchor) {
    }

    private ChassisWalkFrame() {
    }
}
