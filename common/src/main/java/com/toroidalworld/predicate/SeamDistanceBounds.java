package com.toroidalworld.predicate;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// A distance bound — the `distance` of an advancement criterion, and the same field on every EntityPredicate a loot
// table or predicate file carries — is decided by a plain subtraction of two absolute positions. Both of them lie
// inside the world, so that subtraction is never short: it reads the long way round the moment the pair straddles the
// bounds, which lets an `atLeast` bound be satisfied by two points standing next to each other and leaves an `atMost`
// bound impossible to satisfy at all. The error is larger than any threshold vanilla sets.
//
// It cannot be folded where it lives. DistancePredicate.matches is handed six doubles and no level, and the predicate
// itself is a shared immutable record that outlives every level it is asked about — only the criterion putting the
// question knows which world it stands in. So the fold is taken here and each caller hands in the pair it already
// holds, the division of labour SeamRange makes for the positions a mob remembers.
//
// Folding one end of the pair is the whole of it: x, z, horizontal and absolute are all built from the same two
// horizontal differences, and y is passed through untouched, so a vertical bound answers exactly what vanilla answers.
public final class SeamDistanceBounds {
    public static Vec3 nearestCopy(Level level, Vec3 reference, Vec3 measured) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        return transformer == null ? measured : transformer.vectors.nearestCopy(reference, measured);
    }

    private SeamDistanceBounds() {
    }
}
