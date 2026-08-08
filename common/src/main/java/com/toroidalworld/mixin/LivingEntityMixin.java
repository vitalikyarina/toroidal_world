package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldLoopTransformer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

// Knockback is aimed from the attacker's position to the victim's (getSourcePosition minus this) — the same distance-vs-
// direction arithmetic as everywhere else, and raw. Hit across the seam, the attacker reads a whole world away, so the
// victim is flung the long way round instead of away from the blow. Each component is folded to the short way through
// the seam; a same-side hit keeps the plain delta, so ordinary knockback is unchanged.
@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    // A bed whose halves sit on opposite sides of the seam is named by two forms of the same coordinate: the clicked
    // half arrives wrapped, but vanilla derives the other half with a raw relative() that can step past the bounds.
    // Whichever form reaches startSleeping becomes the sleeping position and every placement derived from it — the
    // lie-down pose, the occupied flag, the morning stand-up — so an unwrapped form puts the player a world outside
    // the domain for a tick and the client flashes the unfolded frame. Folding here, and not earlier, is deliberate:
    // the reachability check upstream compares raw distances against where the player actually stands, so the raw
    // form must survive up to it.
    @ModifyVariable(method = "startSleeping", at = @At("HEAD"), argsOnly = true)
    private BlockPos toroidal$wrapBedPosition(BlockPos bedPosition) {
        WorldLoopTransformer transformer = ((TransformerSource) this).toroidal$wrappedTransformer();
        return transformer == null ? bedPosition : transformer.blocks.wrap(bedPosition);
    }
    @ModifyArg(
            method = "hurtServer",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V"),
            index = 1)
    private double toroidal$knockbackDirX(double xd) {
        WorldLoopTransformer transformer = ((TransformerSource) this).toroidal$wrappedTransformer();
        return transformer == null ? xd : transformer.coords.x.foldDelta(xd);
    }

    @ModifyArg(
            method = "hurtServer",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V"),
            index = 2)
    private double toroidal$knockbackDirZ(double zd) {
        WorldLoopTransformer transformer = ((TransformerSource) this).toroidal$wrappedTransformer();
        return transformer == null ? zd : transformer.coords.z.foldDelta(zd);
    }

    // Seeing something is drawn as a line from the eye to it, refused outright past 128 blocks and then clipped against
    // the blocks along the way. Both readings are taken from raw positions, so a player a step away across the seam is
    // half a world off: the range gate alone refuses, and the ray it would have cast crosses the whole map. The point
    // looked at becomes the copy nearest the looker, which is where it visually is — block reads along the ray wrap on
    // their way to a chunk, exactly as they do for the vibration occlusion ray. A target already on this side is
    // untouched, so ordinary sight keeps its exact behaviour.
    @ModifyVariable(
            method = "hasLineOfSight(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/ClipContext$Block;Lnet/minecraft/world/level/ClipContext$Fluid;D)Z",
            at = @At("STORE"), ordinal = 1)
    private Vec3 toroidal$sightTargetThroughSeam(Vec3 to) {
        LivingEntity self = (LivingEntity) (Object) this;
        WorldLoopTransformer transformer = ((TransformerSource) this).toroidal$wrappedTransformer();
        return transformer == null ? to : transformer.vectors.nearestCopy(self.position(), to);
    }
}
