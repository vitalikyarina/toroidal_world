package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;

@Mixin(PathNavigation.class)
public interface PathNavigationAccessor {
    @Accessor("mob")
    Mob toroidal$mob();
}
