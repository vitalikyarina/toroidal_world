package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.projectile.EyeOfEnder;

// The eye is aimed once, at throw time, from the raw difference between the thrower and the stronghold — across the
// seam that points the long way round the world. The target is folded to its nearest copy in both places that read it:
//
// signalTo bakes the direction into a steering point at most 12 blocks out, so the fold must happen before that bake —
// afterwards the wrong direction is all that is left. The folded target may sit a few blocks past the bounds; it is
// only a point to steer from, never ground that is read.
//
// The per-tick steering is folded too, because the eye itself can cross the seam mid-flight: ServerLevelMixin wraps
// every non-player entity back into the world at the end of its tick, which moves the eye a whole world while the
// stored target stays put. The raw per-tick delta would then read a world long — turning the eye around and, because
// the wanted speed chases the delta's length, launching it.
//
// On this game version that steering has no helper to wrap: the difference is taken inline off the tx/tz fields, in the
// middle of the method that also rotates, moves and draws the eye. So the fold is applied to the fields themselves,
// once on entry, which is the same correction in one place instead of at each of their reads — and it is safe to
// write there because they are steering state the throw sets and nothing persists. A target already on this side is
// left at the value it had.
@Mixin(EyeOfEnder.class)
public class EyeOfEnderMixin {
    @Shadow
    private double tx;

    @Shadow
    private double tz;

    @WrapMethod(method = "signalTo")
    private void toroidal$signalThroughSeam(BlockPos target, Operation<Void> original) {
        EyeOfEnder self = (EyeOfEnder) (Object) this;
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(self.level());
        if (transformer == null) {
            original.call(target);
            return;
        }

        BlockPos nearest = transformer.blocks.nearestCopy(self.blockPosition(), target);
        original.call(nearest);
    }

    @Inject(method = "tick()V", at = @At("HEAD"))
    private void toroidal$steerThroughSeam(CallbackInfo ci) {
        EyeOfEnder self = (EyeOfEnder) (Object) this;
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(self.level());
        if (transformer == null) {
            return;
        }

        double nearestX = transformer.coords.x.unwrapAround(self.getX(), this.tx);
        double nearestZ = transformer.coords.z.unwrapAround(self.getZ(), this.tz);
        this.tx = nearestX;
        this.tz = nearestZ;
    }
}
