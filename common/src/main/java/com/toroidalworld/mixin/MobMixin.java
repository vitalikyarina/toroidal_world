package com.toroidalworld.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldLoopTransformer;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// A mob turning towards a target does not go through its look control at all: this is a second copy of the same
// arithmetic, on a plain difference. A skeleton uses it — and since strafing moves it relative to where it faces, one
// that looks the wrong way across the seam backs away from its target as if afraid of the boundary.
@Mixin(Mob.class)
public class MobMixin {
    @ModifyVariable(method = "lookAt(Lnet/minecraft/world/entity/Entity;FF)V", at = @At("STORE"), ordinal = 0)
    private double toroidal$lookDeltaX(double deltaX) {
        WorldLoopTransformer transformer = ((TransformerSource) this).toroidal$wrappedTransformer();
        return transformer == null ? deltaX : transformer.coords.x.foldDelta(deltaX);
    }

    @ModifyVariable(method = "lookAt(Lnet/minecraft/world/entity/Entity;FF)V", at = @At("STORE"), ordinal = 1)
    private double toroidal$lookDeltaZ(double deltaZ) {
        WorldLoopTransformer transformer = ((TransformerSource) this).toroidal$wrappedTransformer();
        return transformer == null ? deltaZ : transformer.coords.z.foldDelta(deltaZ);
    }

    // Reach is asked as a raw overlap between the mob's own box, inflated by its reach, and the target's hitbox. Every
    // step before it already measures through the seam — the mob sees the target, paths to it, turns to face it — and
    // then two sets of coordinates a world apart say nothing is there, so it stands next to its target and never swings.
    // The player's side of the same fight was folded long ago (PlayerMixin), which leaves the seam ringed by a strip
    // where melee only travels one way.
    //
    // The target's hitbox becomes the copy nearest the mob, and vanilla's own comparison runs on that. Both readings
    // this method takes — the reach box and the too-close box a long weapon refuses — are the one value, so folding it
    // where it is fetched keeps them answering about the same target. A target on this side is handed back untouched.
    //
    // Folding the target rather than the attack box is the direction that keeps the rest intact: the attack box is
    // built from the mob's own position and a subclass may narrow it (Ravager does), so it is the frame the question is
    // asked in, not something to move.
    @ModifyExpressionValue(
            method = "isWithinMeleeAttackRange",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getHitbox()Lnet/minecraft/world/phys/AABB;"))
    private AABB toroidal$meleeHitboxThroughSeam(AABB hitbox) {
        WorldLoopTransformer transformer = ((TransformerSource) this).toroidal$wrappedTransformer();
        return transformer == null ? hitbox : transformer.foldBoxToward(((Mob) (Object) this).position(), hitbox);
    }

    // A home is a centre and a radius, and whether something lies inside it is a raw subtraction against that centre.
    // Anchored near the seam, the ground a few steps past the boundary reads a whole world out: the mob standing on it
    // is judged homeless and walked back the way it came, and every stroll candidate that falls over the boundary is
    // thrown away — the radius is a half-circle cut off at the edge of the world rather than a circle on the torus.
    // Homes are far more widely handed out than they look: a leash gives one to whatever it holds, which is why a cow
    // on a rope plants itself at the seam and will not follow.
    //
    // The centre becomes its copy nearest the position being asked about, and vanilla's own comparison runs on that.
    // Folding the centre rather than the distance keeps the two overloads honest about where each measures from — one
    // from the block's corner, one from its middle — and leaves the radius test exactly as it was. A home on this side
    // comes back untouched.
    //
    // Wrapped before it is unwrapped, as SeamSteering does: a home is written down rather than measured, and what was
    // written may sit any number of laps out.
    @ModifyExpressionValue(
            method = "isWithinHome(Lnet/minecraft/core/BlockPos;)Z",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/world/entity/Mob;homePosition:Lnet/minecraft/core/BlockPos;",
                    opcode = Opcodes.GETFIELD))
    private BlockPos toroidal$homeThroughSeam(BlockPos home, @Local(argsOnly = true) BlockPos pos) {
        return toroidal$nearestHome(home, pos);
    }

    @ModifyExpressionValue(
            method = "isWithinHome(Lnet/minecraft/world/phys/Vec3;)Z",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/world/entity/Mob;homePosition:Lnet/minecraft/core/BlockPos;",
                    opcode = Opcodes.GETFIELD))
    private BlockPos toroidal$homeVecThroughSeam(BlockPos home, @Local(argsOnly = true) Vec3 pos) {
        return toroidal$nearestHome(home, BlockPos.containing(pos));
    }

    // The anchor only picks which copy of the home is meant, so rounding the queried point to its block loses nothing:
    // half a block cannot change which side of the world is nearer.
    @Unique
    private BlockPos toroidal$nearestHome(BlockPos home, BlockPos anchor) {
        WorldLoopTransformer transformer = ((TransformerSource) this).toroidal$wrappedTransformer();
        return transformer == null ? home : transformer.blocks.nearestCopy(anchor, home);
    }
}
