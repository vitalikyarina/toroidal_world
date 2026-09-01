package com.toroidalworld.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.phys.Vec3;

@Mixin(targets = "net.minecraft.world.entity.monster.Phantom$PhantomMoveTargetGoal")
public class PhantomMoveTargetGoalMixin {
    @Shadow(aliases = "this$0")
    @Final
    private Phantom phantom;

    @ModifyExpressionValue(
            method = "touchingTarget",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/world/entity/monster/Phantom;moveTargetPoint:Lnet/minecraft/world/phys/Vec3;",
                    opcode = Opcodes.GETFIELD))
    private Vec3 toroidal$arrivalPointThroughSeam(Vec3 moveTargetPoint) {
        return SeamSteering.nearestCopy(this.phantom, moveTargetPoint);
    }
}
