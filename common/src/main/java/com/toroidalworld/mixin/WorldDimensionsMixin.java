package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.gen.ShapedDimensions;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.core.Registry;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;

@Mixin(WorldDimensions.class)
public class WorldDimensionsMixin {
    @ModifyVariable(method = "bake", at = @At("HEAD"), argsOnly = true)
    private Registry<LevelStem> toroidal$restoreStoredShapes(Registry<LevelStem> datapackDimensions) {
        return ShapedDimensions.restoreStoredShapes((WorldDimensions) (Object) this, datapackDimensions);
    }

    @ModifyReturnValue(method = "bake", at = @At("RETURN"))
    private WorldDimensions.Complete toroidal$stampEveryDimension(WorldDimensions.Complete complete) {
        ShapedDimensions.stampDerived(complete.dimensions());
        return complete;
    }
}
