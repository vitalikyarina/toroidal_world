package com.toroidalworld.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.toroidalworld.entity.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.phys.Vec3;

@Mixin(targets = "net.minecraft.world.entity.monster.Phantom$PhantomMoveTargetGoal")
public class PhantomMoveTargetGoalMixin {
    // From the constructor, not shadowed off this$0 — see BeeEnterHiveGoalMixin: the outer reference is javac's, not
    // any mapping set's, so a remapping loader has nothing to resolve it to. The class is abstract, so the constructor
    // runs from whichever concrete goal the phantom was given.
    @Unique
    private Phantom toroidal$phantom;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/monster/Phantom;)V", at = @At("TAIL"))
    private void toroidal$capturePhantom(Phantom phantom, CallbackInfo ci) {
        this.toroidal$phantom = phantom;
    }

    @ModifyExpressionValue(
            method = "touchingTarget",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/world/entity/monster/Phantom;moveTargetPoint:Lnet/minecraft/world/phys/Vec3;",
                    opcode = Opcodes.GETFIELD))
    private Vec3 toroidal$arrivalPointThroughSeam(Vec3 moveTargetPoint) {
        return SeamSteering.nearestCopy(this.toroidal$phantom, moveTargetPoint);
    }
}
