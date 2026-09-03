package com.toroidalworld.compat.create;

import net.minecraft.world.phys.Vec3;

public record FoldedPoint(Object anchorKey, Object targetKey, Vec3 value) {
    public boolean isFor(Object anchorKey, Object targetKey) {
        return this.anchorKey == anchorKey && this.targetKey == targetKey;
    }
}
