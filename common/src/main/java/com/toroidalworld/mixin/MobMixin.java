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
    // Folding the centre rather than the distance leaves the radius test exactly as it was, and keeps the reading
    // honest about where it measures from. A home on this side comes back untouched.
    //
    // Wrapped before it is unwrapped, as SeamSteering does: a home is written down rather than measured, and what was
    // written may sit any number of laps out.
    @ModifyExpressionValue(
            method = "isWithinRestriction(Lnet/minecraft/core/BlockPos;)Z",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/world/entity/Mob;restrictCenter:Lnet/minecraft/core/BlockPos;",
                    opcode = Opcodes.GETFIELD))
    private BlockPos toroidal$homeThroughSeam(BlockPos home, @Local(argsOnly = true) BlockPos pos) {
        return toroidal$nearestHome(home, pos);
    }

    @Unique
    private BlockPos toroidal$nearestHome(BlockPos home, BlockPos anchor) {
        WorldLoopTransformer transformer = ((TransformerSource) this).toroidal$wrappedTransformer();
        return transformer == null ? home : transformer.blocks.nearestCopy(anchor, home);
    }
}
