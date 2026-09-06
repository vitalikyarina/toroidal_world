package com.toroidalworld.compat.create;

import com.toroidalworld.core.WorldFold;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record CreateFrameSeat(Vec3 anchor, WorldFold.Folded<Vec3> seated) {
    public static CreateFrameSeat of(WorldFold transformer, Vec3 viewer, Vec3 anchor) {
        return new CreateFrameSeat(anchor, transformer.nearestCopyOriented(viewer, anchor));
    }

    public boolean isIdentity() {
        return this.seated.isIdentity() && this.seated.value() == this.anchor;
    }

    public Vec3 apply(Vec3 point) {
        if (isIdentity()) {
            return point;
        }

        return this.seated.value().add(this.seated.orientation().applyToDelta(point.subtract(this.anchor)));
    }

    public AABB apply(AABB box) {
        if (isIdentity()) {
            return box;
        }

        return new AABB(apply(new Vec3(box.minX, box.minY, box.minZ)),
                apply(new Vec3(box.maxX, box.maxY, box.maxZ)));
    }
}
