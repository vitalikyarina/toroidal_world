package com.toroidalworld.compat.aeronautics.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.toroidalworld.compat.aeronautics.NavTableSeamFrame;

import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity;

import net.minecraft.world.phys.Vec3;

@Mixin(value = NavTableBlockEntity.class, remap = false)
public class NavTableTargetFrameMixin {
    @ModifyReturnValue(method = "getTargetPosition", at = @At("RETURN"))
    private Vec3 toroidal$seatTargetByTheTable(Vec3 target, @Local(argsOnly = true) boolean project) {
        return project && target != null
                ? NavTableSeamFrame.seatTarget((NavTableBlockEntity) (Object) this, target)
                : target;
    }
}
