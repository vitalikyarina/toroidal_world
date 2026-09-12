package com.toroidalworld.engine.seam;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.ToroidalWorld;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.engine.LogRateGates;
import com.toroidalworld.engine.fold.FoldedBoxQuery;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SeamCollisionBox {
    public static final String SITE_COLLISIONS = "collisions";
    public static final String SITE_UNOBSTRUCTED = "unobstructed";

    private static final LogRateGates PROBE_GATES = new LogRateGates();

    public static AABB toward(@Nullable WorldFold fold, Entity found, AABB testArea, AABB box, String site) {
        Vec3 anchor = testArea.getCenter();
        AABB folded = FoldedBoxQuery.toward(fold, anchor, box);
        if (folded != box && PROBE_GATES.tryPass(site)) {
            ToroidalWorld.LOGGER.info("[seam-collision] fold site={} entity={} raw_min_x={} raw_max_x={} raw_min_z={} "
                            + "raw_max_z={} folded_min_x={} folded_max_x={} folded_min_z={} folded_max_z={} "
                            + "area_center_x={} area_center_z={} moved=true",
                    site, EntityType.getKey(found.getType()), box.minX, box.maxX, box.minZ, box.maxZ,
                    folded.minX, folded.maxX, folded.minZ, folded.maxZ, anchor.x, anchor.z);
        }

        return folded;
    }

    private SeamCollisionBox() {
    }
}
