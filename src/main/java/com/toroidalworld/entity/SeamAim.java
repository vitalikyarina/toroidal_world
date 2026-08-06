package com.toroidalworld.entity;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldLoopTransformer;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

// Where a target is, read from where the shooter stands. Every ranged attacker in the game works its shot out from a
// plain difference between two absolute positions, and across the seam that difference carries the whole world's
// magnitude with the wrong sign — so the shot leaves in the opposite direction, and the arc lift most shooters derive
// from the same difference (sqrt of the horizontal gap, times a fifth) throws it near-vertical on top of that.
//
// Folding each difference would mean knowing, per shooter, every quantity it goes on to derive: the distance for the
// lift, an atan2 for a yaw, a normalize for a beam, a lead on the target's own motion. Handing back the target at its
// copy nearest the shooter instead leaves all of that vanilla's own arithmetic, correct because its inputs now name the
// same world copy. A target already on this side comes back as it came, so an ordinary shot is unchanged.
//
// This is the same primitive SeamSteering hands the walking mob, asked of the shot rather than the step. It is not
// asked of Projectile.shoot, the one place every shot converges: by then the lift is already inside the vertical
// argument and the factor each caller mixed it with is gone.
public final class SeamAim {
    public static double nearX(Entity viewer, double targetX) {
        WorldLoopTransformer transformer = ((TransformerSource) viewer).toroidal$wrappedTransformer();
        return transformer == null ? targetX : transformer.coords.x.unwrapAround(viewer.getX(), targetX);
    }

    public static double nearZ(Entity viewer, double targetZ) {
        WorldLoopTransformer transformer = ((TransformerSource) viewer).toroidal$wrappedTransformer();
        return transformer == null ? targetZ : transformer.coords.z.unwrapAround(viewer.getZ(), targetZ);
    }

    public static Vec3 nearestTo(Entity viewer, Vec3 point) {
        return SeamSteering.nearestCopy(viewer, point);
    }

    // The same measurement where the shooter's own position is out of reach — an inner goal class that holds its mob
    // only through the enclosing instance. A difference needs no reference point to be taken the short way, and an
    // angle is the whole of what these callers build from it; the entity is here to name the level, nothing more.
    public static double foldX(Entity levelSource, double delta) {
        WorldLoopTransformer transformer = ((TransformerSource) levelSource).toroidal$wrappedTransformer();
        return transformer == null ? delta : transformer.coords.x.foldDelta(delta);
    }

    public static double foldZ(Entity levelSource, double delta) {
        WorldLoopTransformer transformer = ((TransformerSource) levelSource).toroidal$wrappedTransformer();
        return transformer == null ? delta : transformer.coords.z.foldDelta(delta);
    }

    // The same difference taken whole, for callers that subtract one position from another and then read the result
    // several ways at once — a length, an angle, a normalized heading. Folding what the subtraction produced leaves
    // every one of those readings vanilla's own. Y has no seam and comes through untouched, and a difference already
    // taking the short way comes back as the object it was.
    public static Vec3 foldDelta(Entity levelSource, Vec3 delta) {
        WorldLoopTransformer transformer = ((TransformerSource) levelSource).toroidal$wrappedTransformer();
        if (transformer == null) {
            return delta;
        }

        double foldedX = transformer.coords.x.foldDelta(delta.x);
        double foldedZ = transformer.coords.z.foldDelta(delta.z);
        return foldedX == delta.x && foldedZ == delta.z ? delta : new Vec3(foldedX, delta.y, foldedZ);
    }

    private SeamAim() {
    }
}
