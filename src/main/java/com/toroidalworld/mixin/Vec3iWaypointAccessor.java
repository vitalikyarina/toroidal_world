package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.core.Vec3i;

// TrackedWaypoint's subclasses are private and keep their coordinate behind a private field, so the packet translator
// can neither see which kind it holds nor read the position — the accessor doubles as both: an instanceof on it tells
// the kind, and the pair below moves the coordinate.
@Mixin(targets = "net.minecraft.world.waypoints.TrackedWaypoint$Vec3iWaypoint")
public interface Vec3iWaypointAccessor {
    @Accessor("vector")
    Vec3i toroidal$getVector();

    @Accessor("vector")
    void toroidal$setVector(Vec3i vector);
}
