package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.phys.Vec3;

@Mixin(EyeOfEnder.class)
public class EyeOfEnderMixin {
    @Shadow
    private double tx;

    @Shadow
    private double tz;

    @WrapMethod(method = "signalTo")
    private void toroidal$signalThroughSeam(BlockPos target, Operation<Void> original) {
        EyeOfEnder self = (EyeOfEnder) (Object) this;
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(self.level());
        if (transformer == null) {
            original.call(target);
            return;
        }

        original.call(transformer.nearestCopy(self.blockPosition(), target));
    }

    @Inject(method = "tick()V", at = @At("HEAD"))
    private void toroidal$steerThroughSeam(CallbackInfo ci) {
        EyeOfEnder self = (EyeOfEnder) (Object) this;
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(self.level());
        if (transformer == null) {
            return;
        }

        Vec3 nearest = transformer.nearestCopy(self.position(), new Vec3(this.tx, self.getY(), this.tz));
        this.tx = nearest.x;
        this.tz = nearest.z;
    }
}
