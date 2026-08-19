package com.toroidalworld.command;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.core.WrapDomain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.commands.arguments.coordinates.WorldCoordinate;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public final class SeamCommandErrors {
    // Escaped as vanilla escapes its own dynamic errors: a non-Component argument makes TranslatableContents throw.
    private static final Dynamic3CommandExceptionType COORDINATE_OUTSIDE_WORLD = new Dynamic3CommandExceptionType(
            (coord, min, max) -> Component.translatableEscape(
                    "argument.toroidal_world.pos.outside_world", coord, min, max));

    private static final SimpleCommandExceptionType REGION_ACROSS_SEAM = new SimpleCommandExceptionType(
            Component.translatable("commands.toroidal_world.region.across_seam"));

    public static void requireInsideWorld(WrapDomain domain, WorldCoordinate coordinate)
            throws CommandSyntaxException {
        if (coordinate.isRelative()) {
            return;
        }

        requireInsideWorld(domain, coordinate.value());
    }

    public static void requireInsideWorld(WrapDomain domain, double coord) throws CommandSyntaxException {
        if (!domain.isOver(coord)) {
            return;
        }

        throw COORDINATE_OUTSIDE_WORLD.create(blockOf(coord), domain.lowerBound, domain.upperBound - 1);
    }

    private static long blockOf(double coord) {
        return (long) Math.floor(coord);
    }

    public static @Nullable CommandSyntaxException refusalForAmbiguousRegion(
            @Nullable WorldLoopTransformer transformer, BoundingBox region) {
        if (transformer == null || !transformer.spansSeam(region)) {
            return null;
        }

        return REGION_ACROSS_SEAM.create();
    }

    public static void requireUnambiguousRegion(@Nullable WorldLoopTransformer transformer, BoundingBox region)
            throws CommandSyntaxException {
        CommandSyntaxException refusal = refusalForAmbiguousRegion(transformer, region);
        if (refusal != null) {
            throw refusal;
        }
    }

    private SeamCommandErrors() {
    }
}
