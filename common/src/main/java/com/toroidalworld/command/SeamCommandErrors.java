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

// What a looped world has to say back to a command, kept out of the mixins that raise it so the wording and the rule
// live in one place.
public final class SeamCommandErrors {
    // Escaped, as vanilla escapes every dynamic command error: a translation argument that is not a Component, Number,
    // Boolean or String makes TranslatableContents throw, which would answer a mistyped coordinate with an exception
    // instead of a sentence. Today's arguments are the coordinate and the two bounds, so it changes nothing — it decides
    // what happens the day one of them stops being a primitive.
    private static final Dynamic3CommandExceptionType COORDINATE_OUTSIDE_WORLD = new Dynamic3CommandExceptionType(
            (coord, min, max) -> Component.translatableEscape(
                    "argument.toroidal_world.pos.outside_world", coord, min, max));

    // Not escaped, and vanilla does not escape its own argument-less errors either: there is nothing to escape.
    private static final SimpleCommandExceptionType REGION_ACROSS_SEAM = new SimpleCommandExceptionType(
            Component.translatable("commands.toroidal_world.region.across_seam"));

    // A coordinate someone typed out is a claim about where a place is, and on a wrapping axis the world only holds the
    // coordinates inside its bounds — so naming one outside them is a mistake worth saying out loud, exactly as vanilla
    // says it at its own thirty-million-block scale. A relative coordinate is not a claim but a movement: walking that
    // far is allowed and lands somewhere real, so it is left alone and wraps.
    //
    // A disabled axis answers isOver false for everything, so a world that wraps on one axis only never refuses on the
    // other.
    public static void requireInsideWorld(WrapDomain domain, WorldCoordinate coordinate)
            throws CommandSyntaxException {
        if (coordinate.isRelative()) {
            return;
        }

        // get() adds the origin only for a relative coordinate, and the branch above has already returned on those, so
        // it hands back the typed value itself. 1.21.1 keeps the field private and offers no other way to read it.
        requireInsideWorld(domain, coordinate.get(0.0));
    }

    // The same rule where the coordinate has already lost its parsed form — a selector's x=/y=/z=, which is as typed
    // and as absolute as anything handed to /tp, but reaches the world as a plain number inside a position function.
    //
    // The message names the number rather than the axis it sits on. The number is what the player wrote and what has to
    // change; the axis they can see from where it stands in their own command — and naming it would mean passing the
    // axis alongside its own domain, two spellings of one fact for nothing to keep them agreeing.
    public static void requireInsideWorld(WrapDomain domain, double coord) throws CommandSyntaxException {
        if (!domain.isOver(coord)) {
            return;
        }

        throw COORDINATE_OUTSIDE_WORLD.create(blockOf(coord), domain.lowerBound, domain.upperBound - 1);
    }

    // The block the coordinate falls in, not the coordinate itself. What is being decided is whether the world holds
    // that block, and the two bounds printed beside it are blocks — a position among them would not be comparable.
    // It is also what the player wrote: vanilla aims Vec3 arguments at block centres, so an absolute coordinate typed
    // without a decimal point arrives here half a block larger than it left their hands.
    private static long blockOf(double coord) {
        return (long) Math.floor(coord);
    }

    // A region whose corners read shorter the other way round names two different places at once, and a command that
    // writes blocks has no business guessing which: the two readings differ by nearly the whole world. Refusing leaves
    // the player to say it plainly — one command per side of the seam — rather than discovering afterwards which half
    // of the world was overwritten.
    //
    // Handed back rather than thrown, because not every command throws: /fillbiome answers in an Either, and it must
    // read the same refusal as /fill and /clone rather than keep its own copy of when one is due.
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
