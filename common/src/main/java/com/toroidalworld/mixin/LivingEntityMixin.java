package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldLoopTransformer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

// Knockback is aimed from the attacker's position to the victim's — the same distance-vs-direction arithmetic as
// everywhere else, and raw. Hit across the seam, the attacker reads a whole world away, so the victim is flung the long
// way round instead of away from the blow. Each component is folded to the short way through the seam; a same-side hit
// keeps the plain delta, so ordinary knockback is unchanged.
@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @ModifyVariable(method = "startSleeping", at = @At("HEAD"), argsOnly = true)
    private BlockPos toroidal$wrapBedPosition(BlockPos bedPosition) {
        WorldLoopTransformer transformer = ((TransformerSource) this).toroidal$wrappedTransformer();
        return transformer == null ? bedPosition : transformer.blocks.wrap(bedPosition);
    }
    // The direction a hit throws something is a plain difference between two absolute positions, so a blow landed across
    // the seam shoves the victim the long way round the world — into the attacker rather than away from it.
    //
    // Folded inside knockback itself rather than at whoever computed the difference: every source of it ends here — the
    // hurt path, a shield deflection, a mob's strike, a player's attack and its sweep, a goat's ram — and one fold on the
    // primitive answers all six. A direction that needed no folding comes back as itself, so an ordinary hit is exactly
    // vanilla; a vector that is already a unit direction rather than a difference is far too short to fold at all.
    @ModifyVariable(method = "knockback(DDD)V", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private double toroidal$knockbackDirX(double xd) {
        WorldLoopTransformer transformer = ((TransformerSource) this).toroidal$wrappedTransformer();
        return transformer == null ? xd : transformer.coords.x.foldDelta(xd);
    }

    @ModifyVariable(method = "knockback(DDD)V", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private double toroidal$knockbackDirZ(double zd) {
        WorldLoopTransformer transformer = ((TransformerSource) this).toroidal$wrappedTransformer();
        return transformer == null ? zd : transformer.coords.z.foldDelta(zd);
    }

    @ModifyVariable(
            method = "hasLineOfSight(Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("STORE"), ordinal = 1)
    private Vec3 toroidal$sightTargetThroughSeam(Vec3 to) {
        LivingEntity self = (LivingEntity) (Object) this;
        WorldLoopTransformer transformer = ((TransformerSource) this).toroidal$wrappedTransformer();
        return transformer == null ? to : transformer.vectors.nearestCopy(self.position(), to);
    }
}
