package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.equipment.symmetryWand.SymmetryHandler;
import com.toroidalworld.client.ClientFrame;

import net.minecraft.world.phys.Vec3;

@Mixin(value = SymmetryHandler.class, remap = false)
public abstract class SymmetryHandlerMixin {
    @Unique
    private static final String MIRROR_POSITION =
            "Lcom/simibubi/create/content/equipment/symmetryWand/mirror/SymmetryMirror;"
                    + "getPosition()Lnet/minecraft/world/phys/Vec3;";

    @ModifyExpressionValue(method = "onRenderWorld", at = @At(value = "INVOKE", target = MIRROR_POSITION))
    private static Vec3 toroidal$mirrorInTheRenderFrame(Vec3 canonical) {
        return ClientFrame.nearestToCamera(canonical);
    }

    @ModifyExpressionValue(method = "onClientTick", at = @At(value = "INVOKE", target = MIRROR_POSITION))
    private static Vec3 toroidal$mirrorInTheParticleFrame(Vec3 canonical) {
        return ClientFrame.nearestToCamera(canonical);
    }
}
