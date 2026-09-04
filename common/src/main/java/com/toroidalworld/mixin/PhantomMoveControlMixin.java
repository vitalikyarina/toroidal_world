package com.toroidalworld.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.toroidalworld.InjectionTargets;
import com.toroidalworld.entity.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.phys.Vec3;

@Mixin(targets = "net.minecraft.world.entity.monster.Phantom$PhantomMoveControl")
public class PhantomMoveControlMixin {
    // From the constructor, not shadowed off this$0 — see BeeEnterHiveGoalMixin: the outer reference is javac's, not
    // any mapping set's, so a remapping loader has nothing to resolve it to. The second argument is the mob the control
    // steers, which vanilla hands to MoveControl and this fold has no use for.
    @Unique
    private Phantom toroidal$phantom;

    @Inject(
            method = "<init>(Lnet/minecraft/world/entity/monster/Phantom;Lnet/minecraft/world/entity/Mob;)V",
            at = @At("TAIL"))
    private void toroidal$capturePhantom(Phantom phantom, Mob mob, CallbackInfo ci) {
        this.toroidal$phantom = phantom;
    }

    @ModifyExpressionValue(
            method = "tick",
            at = @At(value = "FIELD",
                    target = InjectionTargets.PHANTOM_MOVE_TARGET_POINT,
                    opcode = Opcodes.GETFIELD))
    private Vec3 toroidal$moveTargetThroughSeam(Vec3 moveTargetPoint) {
        return SeamSteering.nearestCopy(this.toroidal$phantom, moveTargetPoint);
    }
}
