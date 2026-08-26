package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.phys.Vec3;

@Mixin(EyeOfEnder.class)
public class EyeOfEnderMixin {
    @Unique
    private static final String UPDATE_DELTA_MOVEMENT =
            "Lnet/minecraft/world/entity/projectile/EyeOfEnder;updateDeltaMovement(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;";

    @WrapMethod(method = "signalTo")
    private void toroidal$signalThroughSeam(Vec3 target, Operation<Void> original) {
        EyeOfEnder self = (EyeOfEnder) (Object) this;
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(self.level());
        if (transformer == null) {
            original.call(target);
            return;
        }

        original.call(transformer.nearestCopy(self.position(), target));
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = UPDATE_DELTA_MOVEMENT))
    private Vec3 toroidal$steerThroughSeam(Vec3 movement, Vec3 position, Vec3 target, Operation<Vec3> original) {
        EyeOfEnder self = (EyeOfEnder) (Object) this;
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(self.level());
        if (transformer == null) {
            return original.call(movement, position, target);
        }

        return original.call(movement, position, transformer.nearestCopy(position, target));
    }
}
