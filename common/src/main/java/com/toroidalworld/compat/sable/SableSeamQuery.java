package com.toroidalworld.compat.sable;

import com.toroidalworld.core.WorldFold;

import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SableSeamQuery {
    public static boolean intersects(WorldFold fold, BoundingBox3dc subLevelBox, BoundingBox3dc query) {
        Vec3 queryCentre = new Vec3(
                (query.minX() + query.maxX()) / 2.0,
                (query.minY() + query.maxY()) / 2.0,
                (query.minZ() + query.maxZ()) / 2.0);
        AABB subLevelAabb = new AABB(subLevelBox.minX(), subLevelBox.minY(), subLevelBox.minZ(),
                subLevelBox.maxX(), subLevelBox.maxY(), subLevelBox.maxZ());
        AABB nearest = fold.foldBox(queryCentre, subLevelAabb).value();
        double shiftX = nearest.minX - subLevelAabb.minX;
        double shiftZ = nearest.minZ - subLevelAabb.minZ;
        if (shiftX == 0.0 && shiftZ == 0.0) {
            return subLevelBox.intersects(query);
        }

        return subLevelBox.move(shiftX, 0.0, shiftZ, new BoundingBox3d()).intersects(query);
    }

    private SableSeamQuery() {
    }
}
