package com.toroidalworld.compat.sable;

import org.joml.Vector3d;

import com.toroidalworld.core.DeckTransformation;

final class SableRigidShift {
    static void requireTranslation(DeckTransformation seat) {
        if (!seat.orientation().isIdentity()) {
            throw new IllegalStateException("A Sable body crosses a seam by translation alone, and this seam applies "
                    + seat.orientation() + ": a reflection is not a rigid-body rotation");
        }
    }

    static Vector3d translationOf(DeckTransformation seat) {
        requireTranslation(seat);
        return new Vector3d(seat.blocks().xShift(), 0.0, seat.blocks().zShift());
    }

    private SableRigidShift() {
    }
}
