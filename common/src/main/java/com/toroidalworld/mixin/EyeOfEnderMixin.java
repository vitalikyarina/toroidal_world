package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.phys.Vec3;

// The eye is aimed once, at throw time, from the raw difference between the thrower and the stronghold — across the
// seam that points the long way round the world. The target is folded to its nearest copy in both places that read it:
//
// signalTo bakes the direction into a steering point at most 12 blocks out, so the fold must happen before that bake —
// afterwards the wrong direction is all that is left. The folded steering point may sit a few blocks past the bounds;
// it is only a point to fly toward, never ground that is read.
//
// The per-tick steering is folded too, because the eye itself can cross the seam mid-flight: ServerLevelMixin wraps
// every non-player entity back into the world at the end of its tick, which moves the eye a whole world while the
// stored target stays put. The raw per-tick delta would then read a world long — turning the eye around and, because
// the wanted speed chases the delta's length, launching it. Folding the target around the current position each tick
// keeps the delta short no matter which side of the seam the eye woke up on.
@Mixin(EyeOfEnder.class)
public class EyeOfEnderMixin {
    @Unique
    private static final String UPDATE_DELTA_MOVEMENT =
            "Lnet/minecraft/world/entity/projectile/EyeOfEnder;updateDeltaMovement(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;";

    @WrapMethod(method = "signalTo")
    private void toroidal$signalThroughSeam(Vec3 target, Operation<Void> original) {
        EyeOfEnder self = (EyeOfEnder) (Object) this;
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(self.level());
        if (transformer == null) {
            original.call(target);
            return;
        }

        original.call(transformer.vectors.nearestCopy(self.position(), target));
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = UPDATE_DELTA_MOVEMENT))
    private Vec3 toroidal$steerThroughSeam(Vec3 movement, Vec3 position, Vec3 target, Operation<Vec3> original) {
        EyeOfEnder self = (EyeOfEnder) (Object) this;
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(self.level());
        if (transformer == null) {
            return original.call(movement, position, target);
        }

        return original.call(movement, position, transformer.vectors.nearestCopy(position, target));
    }
}
