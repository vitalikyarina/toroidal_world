package com.toroidalworld.engine.noise;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

// Stands in for DensityFunction.mapChildren, which this game version splits out of the recursive mapAll.
public final class DensityChildren {
    private DensityChildren() {
    }

    public static List<DensityFunction> of(DensityFunction node) {
        return switch (node) {
            case DensityFunctions.TwoArgumentSimpleFunction function ->
                    List.of(function.argument1(), function.argument2());
            case DensityFunctions.MarkerOrMarked function -> List.of(function.wrapped());
            case DensityFunctions.HolderHolder(Holder<DensityFunction> holder) ->
                    holder.isBound() ? List.of(holder.value()) : List.of();
            case DensityFunctions.PureTransformer function -> List.of(function.input());
            case DensityFunctions.TransformerWithContext function -> List.of(function.input());
            case DensityFunctions.RangeChoice function ->
                    List.of(function.input(), function.whenInRange(), function.whenOutOfRange());
            case DensityFunctions.ShiftedNoise function ->
                    List.of(function.shiftX(), function.shiftY(), function.shiftZ());
            case DensityFunctions.FindTopSurface function -> List.of(function.density(), function.upperBound());
            case DensityFunctions.Spline function -> boundCoordinates(function);
            default -> List.of();
        };
    }

    private static List<DensityFunction> boundCoordinates(DensityFunctions.Spline spline) {
        List<DensityFunction> children = new ArrayList<>();
        spline.spline().mapAll(coordinate -> {
            Holder<DensityFunction> holder = coordinate.function();
            if (holder.isBound()) {
                children.add(holder.value());
            }

            return coordinate;
        });

        return children;
    }
}
