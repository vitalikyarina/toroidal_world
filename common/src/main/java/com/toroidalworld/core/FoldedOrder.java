package com.toroidalworld.core;

import java.util.Comparator;
import java.util.function.UnaryOperator;

import net.minecraft.core.BlockPos;

public final class FoldedOrder {
    public static <T> Comparator<T> of(Comparator<T> original, UnaryOperator<T> fold) {
        return (first, second) -> original.compare(fold.apply(first), fold.apply(second));
    }

    public static Comparator<BlockPos> around(Comparator<BlockPos> original, WorldFold fold, BlockPos ref) {
        return of(original, target -> fold.nearestCopy(ref, target));
    }

    private FoldedOrder() {
    }
}
