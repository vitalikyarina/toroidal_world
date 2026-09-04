package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.entity.SeamAim;

import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.phys.Vec3;

@Mixin(ExperienceOrb.class)
public class ExperienceOrbAimMixin {
    // On 1.21.1 the orb homes on its player inside tick itself; 26.x later split that body out as followNearbyPlayer.
    // tick holds exactly one Vec3 local, the delta this folds, so the STORE needs no ordinal.
    @ModifyVariable(method = "tick", at = @At("STORE"))
    private Vec3 toroidal$followDeltaThroughSeam(Vec3 delta) {
        return SeamAim.foldDelta((ExperienceOrb) (Object) this, delta);
    }
}
