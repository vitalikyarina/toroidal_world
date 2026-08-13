package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.LongJumpUtil;
import net.minecraft.world.phys.Vec3;

// The whole of the leap is worked out here — every mob that jumps a distance rather than walking it comes through this
// one method, and everything it derives comes from the difference between where the mob is and where it means to land:
// the heading, the range that sets the launch speed, and the arc the collision samples are walked along. Across the
// seam that difference is a world wide with the wrong sign, so the range makes the jump impossible and the mob simply
// refuses to leap at a landing spot a few blocks past the boundary.
//
// The landing spot becomes its copy nearest the jumper, which is where it physically is. The two callers split the
// mechanism: the breeze aims at a point built in its victim's frame — the one target that genuinely folds — and skips
// the collision samples; the frog and the goat pick candidates in their own frame, so their fold is a no-op, while
// their samples may still reach past the bounds beside the boundary, where block reads wrap on their way to a chunk.
// A landing spot on this side arrives unchanged and jumps precisely as vanilla jumps.
@Mixin(LongJumpUtil.class)
public class LongJumpUtilMixin {
    @ModifyVariable(method = "calculateJumpVectorForAngle", at = @At("HEAD"), argsOnly = true)
    private static Vec3 toroidal$jumpTargetThroughSeam(Vec3 targetPos, @Local(argsOnly = true) Mob body) {
        return SeamAim.nearestTo(body, targetPos);
    }
}
