package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.AbstractDragonPhaseInstance;

@Mixin(AbstractDragonPhaseInstance.class)
public interface DragonPhaseAccessor {
    @Accessor("dragon")
    EnderDragon toroidal$dragon();
}
