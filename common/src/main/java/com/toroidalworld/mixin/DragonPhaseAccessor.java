package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.AbstractDragonPhaseInstance;

// A phase works its attack out from the dragon it belongs to, but keeps it protected and offers no reader — and the
// phases that need folding are subclasses, where the field is inherited rather than declared. The accessor sits on the
// class that declares it, so there is one unambiguous answer to where it comes from.
@Mixin(AbstractDragonPhaseInstance.class)
public interface DragonPhaseAccessor {
    @Accessor("dragon")
    EnderDragon toroidal$dragon();
}
