package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldFold;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.BlockUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

// Mixin refuses to inject into a method a mixin of equal priority overwrote; Sable overwrites this one at 1000.
@Mixin(value = Entity.class, priority = 1100)
public class EntityPortalPositionMixin {
    @ModifyArg(
            method = "getRelativePortalPosition",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/portal/PortalShape;getRelativePosition(Lnet/minecraft/BlockUtil$FoundRectangle;Lnet/minecraft/core/Direction$Axis;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/entity/EntityDimensions;)Lnet/minecraft/world/phys/Vec3;"),
            index = 2)
    private Vec3 toroidal$portalPositionNearestCorner(Vec3 position, @Local(argsOnly = true) BlockUtil.FoundRectangle portalArea) {
        WorldFold transformer = ((TransformerSource) (Object) this).toroidal$wrappedTransformer();
        if (transformer == null) {
            return position;
        }

        return transformer.nearestCopy(Vec3.atLowerCornerOf(portalArea.minCorner), position);
    }
}
