package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldLoopTransformer;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityAccess;

// Where an entity is filed once it moves. The section is keyed off the entity's own coordinate, so one pushed a step
// past the bounds is filed under a chunk the world never loads — a section born HIDDEN, which stops the entity ticking
// and stops it being tracked.
//
// That is a trap with no exit. The wrap that brings a strayed entity home runs at the tail of its tick, so an entity
// dropped out of the ticking set can never be reached by it again: it freezes where it stands, invisible, unreachable
// by any query — the pickup search looks in the wrapped chunk while the entity sits in the raw one — and is saved to a
// region file nothing will ever ask for. A piston pushing a fresh drop out of the world does exactly this before the
// item has ticked even once.
//
// Only the filing is corrected, not the coordinate: the entity keeps its continuous position through the move, which
// is what collision resolution and the fold helpers read, and the tick tail normalises it a moment later — now that it
// is still ticking to reach that tail.
@Mixin(targets = "net/minecraft/world/level/entity/PersistentEntitySectionManager$Callback")
public class EntitySectionCallbackMixin {
    // Vanilla's own field: the holder keeps the entity it files as the manager's type parameter. NeoForge adds an
    // Entity-typed realEntity beside it but keeps this one, so the one name serves both loaders.
    @Shadow
    @Final
    private EntityAccess entity;

    @ModifyExpressionValue(
            method = "onMove",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/SectionPos;asLong(Lnet/minecraft/core/BlockPos;)J"))
    private long toroidal$fileInPhysicalSection(long sectionKey) {
        if (!(this.entity instanceof Entity actualEntity)) {
            return sectionKey;
        }

        WorldLoopTransformer transformer = ((TransformerSource) actualEntity).toroidal$wrappedTransformer();
        return transformer == null ? sectionKey : transformer.chunks.wrapSectionNode(sectionKey);
    }
}
