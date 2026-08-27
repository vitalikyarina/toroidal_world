package com.toroidalworld.compat.sable.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.SubLevel;

@Mixin(value = SubLevel.class, remap = false)
public interface SubLevelAccessor {
    @Accessor("lastPose")
    Pose3d toroidal$lastPose();
}
