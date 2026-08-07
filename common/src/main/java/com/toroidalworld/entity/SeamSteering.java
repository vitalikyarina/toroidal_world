package com.toroidalworld.entity;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldLoopTransformer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

// Knowing where something is does not say which way to walk to it. The whole RandomPos family works the destination out
// from a plain difference between two absolute positions and turns that into an angle, so across the seam the difference
// carries the opposite sign and the angle points the long way round the world. Every candidate is then drawn from a
// half-plane about it, which makes the error total rather than approximate: all ten attempts land behind the mob, and a
// mob fleeing a threat a few blocks away across the boundary runs into it.
//
// The steering position becomes its copy nearest the mob — where it physically is — so the difference the family then
// takes is the short way through the seam. A target already on this side comes back untouched and steers exactly as
// vanilla does.
//
// The walking mob is where this is asked most, but nothing in the answer is about walking: the reference point is a
// position, and a bat drifting to a perch or a villager naming the doorway another villager is standing in needs the
// same copy chosen the same way.
public final class SeamSteering {
    public static Vec3 nearestCopy(Entity body, Vec3 target) {
        WorldLoopTransformer transformer = ((TransformerSource) body).toroidal$wrappedTransformer();
        return transformer == null ? target : transformer.vectors.nearestCopy(body.position(), target);
    }

    // The same question asked of a block — of a home or a memory the mob keeps rather than measures.
    public static BlockPos nearestCopy(Entity body, BlockPos target) {
        WorldLoopTransformer transformer = ((TransformerSource) body).toroidal$wrappedTransformer();
        return transformer == null ? target : transformer.blocks.nearestCopy(body.blockPosition(), target);
    }

    private SeamSteering() {
    }
}
