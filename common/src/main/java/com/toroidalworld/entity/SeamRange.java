package com.toroidalworld.entity;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// How far apart two places are when neither of them is an entity. Entity's own five distance methods already measure
// through the seam (EntityMixin), which covers everything the game asks about something it can see; what is left is
// everything it asks about something it remembers — a bed, a job site, a meeting point, a hiding place — where both
// endpoints are plain coordinates and the reading is a bare subtraction between them.
//
// That subtraction cannot be folded where it lives: a BlockPos carries no world, and the very same methods measure
// chunk and structure coordinates, which have no seam to fold. Only the behaviour asking the question knows which level
// it stands in, so the fold is taken here and each caller hands in the two points it already held. The entity is there
// to name the level, nothing more — a difference needs no reference point to be taken the short way.
//
// Every method answers what its vanilla counterpart would, and on a level that does not loop it answers by calling it.
public final class SeamRange {
    public static int manhattan(Entity levelSource, Vec3i from, Vec3i to) {
        WorldLoopTransformer transformer = transformerOf(levelSource);
        if (transformer == null) {
            return from.distManhattan(to);
        }

        return Math.abs(transformer.coords.x.foldDelta(to.getX() - from.getX()))
                + Math.abs(to.getY() - from.getY())
                + Math.abs(transformer.coords.z.foldDelta(to.getZ() - from.getZ()));
    }

    public static double sqr(Entity levelSource, Vec3i from, Vec3i to) {
        WorldLoopTransformer transformer = transformerOf(levelSource);
        if (transformer == null) {
            return from.distSqr(to);
        }

        return transformer.coords.sqrDistToBounds(
                from.getX(), from.getY(), from.getZ(),
                to.getX(), to.getY(), to.getZ());
    }

    // The same fold where the asker holds the level itself rather than an entity — a block entity, saved data, the
    // event dispatcher. The level names itself; everything else reads as the Entity-keyed twin.
    public static double sqr(Level levelSource, Vec3i from, Vec3i to) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(levelSource);
        if (transformer == null) {
            return from.distSqr(to);
        }

        return transformer.coords.sqrDistToBounds(
                from.getX(), from.getY(), from.getZ(),
                to.getX(), to.getY(), to.getZ());
    }

    public static double sqr(Entity levelSource, Vec3 from, Position to) {
        WorldLoopTransformer transformer = transformerOf(levelSource);
        if (transformer == null) {
            return from.distanceToSqr(to.x(), to.y(), to.z());
        }

        return transformer.coords.sqrDistToBounds(from.x, from.y, from.z, to.x(), to.y(), to.z());
    }

    public static double sqr(Level levelSource, Vec3 from, Position to) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(levelSource);
        if (transformer == null) {
            return from.distanceToSqr(to.x(), to.y(), to.z());
        }

        return transformer.coords.sqrDistToBounds(from.x, from.y, from.z, to.x(), to.y(), to.z());
    }

    // The range tests are vanilla's own, word for word, taken on the folded distance: both compare against the squared
    // threshold rather than a rooted one, so a caller passing an int reads exactly as it read before.
    public static boolean closerThan(Entity levelSource, Vec3i from, Vec3i to, double distance) {
        return sqr(levelSource, from, to) < Mth.square(distance);
    }

    public static boolean closerThan(Entity levelSource, Vec3 from, Position to, double distance) {
        return sqr(levelSource, from, to) < Mth.square(distance);
    }

    public static boolean closerThan(Level levelSource, Vec3i from, Vec3i to, double distance) {
        return sqr(levelSource, from, to) < Mth.square(distance);
    }

    // What almost every caller here actually asks: a block against a body standing somewhere. Vanilla measures a block
    // from its centre, so the caller hands in the block it holds and the centring belongs to the fold, not to each of
    // the two dozen places that need it.
    public static boolean closerToCenterThan(Entity levelSource, Vec3i from, Position to, double distance) {
        return closerThan(levelSource, Vec3.atCenterOf(from), to, distance);
    }

    private static @Nullable WorldLoopTransformer transformerOf(Entity levelSource) {
        return ((TransformerSource) levelSource).toroidal$wrappedTransformer();
    }

    private SeamRange() {
    }
}
