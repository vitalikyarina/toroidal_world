package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.gen.ShapedDimensions;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.world.level.levelgen.WorldDimensions;

@Mixin(WorldDimensions.class)
public class WorldDimensionsMixin {
    @ModifyReturnValue(method = "bake", at = @At("RETURN"))
    private WorldDimensions.Complete toroidal$stampEveryDimension(WorldDimensions.Complete complete) {
        ShapedDimensions.stampDerived(complete.dimensions());
        return complete;
    }
}
