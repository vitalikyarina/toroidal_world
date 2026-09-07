package com.toroidalworld.noise;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

// 1.21.1 has no DensityFunction.mapChildren: mapAll is recursive and rebuilds every HolderHolder onto a direct
// holder, which drops the resource key a search matches on.
public final class DensityFunctionChildren {
    public static List<DensityFunction> of(DensityFunction node) {
        List<DensityFunction> children = new ArrayList<>();
        switch (node) {
            case DensityFunctions.MarkerOrMarked marked -> children.add(marked.wrapped());
            case DensityFunctions.TwoArgumentSimpleFunction two -> {
                children.add(two.argument1());
                children.add(two.argument2());
            }
            case DensityFunctions.PureTransformer pure -> children.add(pure.input());
            case DensityFunctions.TransformerWithContext contextual -> children.add(contextual.input());
            case DensityFunctions.RangeChoice choice -> {
                children.add(choice.input());
                children.add(choice.whenInRange());
                children.add(choice.whenOutOfRange());
            }
            case DensityFunctions.ShiftedNoise shifted -> {
                children.add(shifted.shiftX());
                children.add(shifted.shiftY());
                children.add(shifted.shiftZ());
            }
            case DensityFunctions.HolderHolder holder -> {
                if (holder.function().isBound()) {
                    children.add(holder.function().value());
                }
            }
            case DensityFunctions.Spline spline -> spline.spline().mapAll(coordinate -> {
                if (coordinate.function().isBound()) {
                    children.add(coordinate.function().value());
                }

                return coordinate;
            });
            default -> {
            }
        }

        return children;
    }

    private DensityFunctionChildren() {
    }
}
