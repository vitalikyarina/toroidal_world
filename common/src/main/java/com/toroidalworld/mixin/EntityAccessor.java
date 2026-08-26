package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(Entity.class)
public interface EntityAccessor {
    @Accessor("lastKnownPosition")
    @Nullable Vec3 toroidal$lastKnownPosition();

    @Accessor("lastKnownPosition")
    void toroidal$setLastKnownPosition(@Nullable Vec3 position);
}
