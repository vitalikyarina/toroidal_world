package com.toroidalworld.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.InjectionTargets;
import com.toroidalworld.entity.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.phys.Vec3;

@Mixin(targets = "net.minecraft.world.entity.monster.Phantom$PhantomMoveControl")
public class PhantomMoveControlMixin {
    @Shadow(aliases = "this$0")
    @Final
    private Phantom phantom;

    @ModifyExpressionValue(
            method = "tick",
            at = @At(value = "FIELD",
                    target = InjectionTargets.PHANTOM_MOVE_TARGET_POINT,
                    opcode = Opcodes.GETFIELD))
    private Vec3 toroidal$moveTargetThroughSeam(Vec3 moveTargetPoint) {
        return SeamSteering.nearestCopy(this.phantom, moveTargetPoint);
    }
}
