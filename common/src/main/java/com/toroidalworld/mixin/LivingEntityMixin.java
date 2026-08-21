package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.entity.SeamAim;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @ModifyVariable(method = "startSleeping", at = @At("HEAD"), argsOnly = true)
    private BlockPos toroidal$wrapBedPosition(BlockPos bedPosition) {
        WorldLoopTransformer transformer = ((TransformerSource) this).toroidal$wrappedTransformer();
        return transformer == null ? bedPosition : transformer.blocks.wrap(bedPosition);
    }

    @ModifyVariable(method = "knockback(DDD)V", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private double toroidal$knockbackDirX(double xd) {
        return SeamAim.foldX((LivingEntity) (Object) this, xd);
    }

    @ModifyVariable(method = "knockback(DDD)V", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private double toroidal$knockbackDirZ(double zd) {
        return SeamAim.foldZ((LivingEntity) (Object) this, zd);
    }

    // On 1.21.1 the shield cone is measured in isDamageSourceBlocked, off vectorTo; 26.x moved it into
    // applyItemBlocking and spells it as a subtract.
    @ModifyExpressionValue(
            method = "isDamageSourceBlocked",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;vectorTo(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$blockConeThroughSeam(Vec3 attackDirection) {
        return SeamAim.foldDelta((LivingEntity) (Object) this, attackDirection);
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
