package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.map.MapSeamFold;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

@Mixin(MapItemSavedData.class)
public class MapItemSavedDataMixin {
    @Shadow
    @Final
    public int centerX;

    @Shadow
    @Final
    public int centerZ;

    @Shadow
    @Final
    public ResourceKey<Level> dimension;

    @ModifyVariable(method = "addDecoration", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private double toroidal$foldDecorationX(double xPos, @Local(argsOnly = true) @Nullable LevelAccessor level) {
        WorldLoopTransformer transformer = MapSeamFold.transformerFor(level, this.dimension);
        return transformer == null ? xPos : transformer.coords.x.unwrapAround(this.centerX, xPos);
    }

    @ModifyVariable(method = "addDecoration", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private double toroidal$foldDecorationZ(double zPos, @Local(argsOnly = true) @Nullable LevelAccessor level) {
        WorldLoopTransformer transformer = MapSeamFold.transformerFor(level, this.dimension);
        return transformer == null ? zPos : transformer.coords.z.unwrapAround(this.centerZ, zPos);
    }

    @ModifyVariable(method = "toggleBanner", at = @At("STORE"), ordinal = 0)
    private double toroidal$foldBannerX(double xPos, @Local(argsOnly = true) LevelAccessor level) {
        WorldLoopTransformer transformer = MapSeamFold.transformerFor(level, this.dimension);
        return transformer == null ? xPos : transformer.coords.x.unwrapAround(this.centerX, xPos);
    }

    @ModifyVariable(method = "toggleBanner", at = @At("STORE"), ordinal = 1)
    private double toroidal$foldBannerZ(double zPos, @Local(argsOnly = true) LevelAccessor level) {
        WorldLoopTransformer transformer = MapSeamFold.transformerFor(level, this.dimension);
        return transformer == null ? zPos : transformer.coords.z.unwrapAround(this.centerZ, zPos);
    }
}
