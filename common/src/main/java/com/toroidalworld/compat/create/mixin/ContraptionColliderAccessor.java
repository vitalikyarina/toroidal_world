package com.toroidalworld.compat.create.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.entity.Entity;

// Who the contraption is carrying. The field sits on AbstractContraptionEntity, which the loader-free module cannot
// name — the class implements a NeoForge interface — so the target is given as a string and the member reached through
// this interface instead.
@Mixin(targets = "com.simibubi.create.content.contraptions.AbstractContraptionEntity", remap = false)
public interface ContraptionColliderAccessor {
    @Accessor("collidingEntities")
    Map<Entity, ?> toroidal$collidingEntities();
}
