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
