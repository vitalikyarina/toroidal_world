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

// The gate that says the phantom has arrived, and the only thing that ever replaces the point it is flying at: a raw
// distance from the point to the phantom, against two blocks. Across the seam it reads a whole world, so the gate can
// never fire — the circle goal keeps the one point it chose forever, and the phantom flies away from it for as long as
// it lives.
//
// This is the same field the move control reads, folded the same way, and the base goal is where the reading lives:
// neither the circling goal nor the sweep goal overrides it, so both arrive through this one method.
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
