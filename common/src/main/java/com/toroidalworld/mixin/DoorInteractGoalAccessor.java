package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.DoorInteractGoal;

@Mixin(DoorInteractGoal.class)
public interface DoorInteractGoalAccessor {
    @Accessor("mob")
    Mob toroidal$mob();
}
