package com.toroidalworld.compat.create.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.entity.Entity;

// Who the contraption is carrying. The field sits on AbstractContraptionEntity, which the loader-free module cannot
// name — the class implements a NeoForge interface — so the target is given as a string and the member reached through
// this interface instead.
//
// collidingEntities is Create's own answer to "resting on my surface": the collider files an entry only under
// surfaceCollision and drops it three ticks after the contact ends. That is the set which has to travel when the
// carriage is renamed — a box drawn around the deck by us would take in whoever happens to stand beside the track.
@Mixin(targets = "com.simibubi.create.content.contraptions.AbstractContraptionEntity", remap = false)
public interface ContraptionColliderAccessor {
    @Accessor("collidingEntities")
    Map<Entity, ?> toroidal$collidingEntities();
}
