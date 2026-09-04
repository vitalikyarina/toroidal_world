package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.entity.SeamAim;

import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.phys.Vec3;

@Mixin(ExperienceOrb.class)
public class ExperienceOrbAimMixin {
    @ModifyVariable(method = "followNearbyPlayer", at = @At("STORE"))
    private Vec3 toroidal$followDeltaThroughSeam(Vec3 delta) {
        return SeamAim.foldDelta((ExperienceOrb) (Object) this, delta);
    }
}
