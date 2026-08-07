package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.ai.behavior.warden.SonicBoom;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.phys.Vec3;

// The warden's boom is a straight line from its chest to the target's eyes, and everything about it comes from that one
// vector: the particles are laid along it a block apart, its length says how many, and the knockback pushes the victim
// along it. Across the seam it points the long way round — so the shove throws the player toward the boundary instead
// of away from the warden, and the trail is drawn half a world long, which is also several hundred particle packets
// sent for one roar. The reach test above it is already folded, so the boom does fire; only its direction was raw.
//
// The boom is written inside a lambda, which compiles to a method of its own — so unlike every other fold in this
// family, this one names a synthetic method and will stop matching if vanilla's lambda ordering shifts. That failure
// is loud: mixin refuses to apply and the game says so at startup.
@Mixin(SonicBoom.class)
public class SonicBoomAimMixin {
    @ModifyExpressionValue(
            method = "lambda$tick$2",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getEyePosition()Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 toroidal$boomTargetThroughSeam(Vec3 eyePosition, @Local(argsOnly = true) Warden body) {
        return SeamAim.nearestTo(body, eyePosition);
    }
}
