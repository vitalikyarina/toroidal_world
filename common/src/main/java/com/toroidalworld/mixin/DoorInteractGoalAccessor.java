package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.DoorInteractGoal;

// The door goals keep the mob they belong to protected and offer no reader, and the one that needs folding is a
// subclass, where the field is inherited rather than declared. The accessor sits on the class that declares it, so
// there is one unambiguous answer to where it comes from.
@Mixin(DoorInteractGoal.class)
public interface DoorInteractGoalAccessor {
    @Accessor("mob")
    Mob toroidal$mob();
}
