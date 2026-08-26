package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.map.MapSeamFold;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;

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
    private double toroidal$foldDecorationX(double xPos, @Local(argsOnly = true) @Nullable LevelAccessor level,
            @Local(argsOnly = true, ordinal = 1) double zPos, @Share("decoration") LocalRef<Vec3> nearest) {
        WorldFold transformer = MapSeamFold.transformerFor(level, this.dimension);
        return transformer == null ? xPos : toroidal$nearestDecoration(transformer, xPos, zPos, nearest).x;
    }

    @ModifyVariable(method = "addDecoration", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private double toroidal$foldDecorationZ(double zPos, @Local(argsOnly = true) @Nullable LevelAccessor level,
            @Local(argsOnly = true, ordinal = 0) double xPos, @Share("decoration") LocalRef<Vec3> nearest) {
        WorldFold transformer = MapSeamFold.transformerFor(level, this.dimension);
        return transformer == null ? zPos : toroidal$nearestDecoration(transformer, xPos, zPos, nearest).z;
    }

    @ModifyVariable(method = "toggleBanner", at = @At("STORE"), ordinal = 0)
    private double toroidal$foldBannerX(double xPos, @Local(argsOnly = true) LevelAccessor level) {
        WorldFold transformer = MapSeamFold.transformerFor(level, this.dimension);
        return transformer == null ? xPos : transformer.blockDomain(Direction.Axis.X).unwrapAround(this.centerX, xPos);
    }

    @ModifyVariable(method = "toggleBanner", at = @At("STORE"), ordinal = 1)
    private double toroidal$foldBannerZ(double zPos, @Local(argsOnly = true) LevelAccessor level) {
        WorldFold transformer = MapSeamFold.transformerFor(level, this.dimension);
        return transformer == null ? zPos : transformer.blockDomain(Direction.Axis.Z).unwrapAround(this.centerZ, zPos);
    }

    @Unique
    private Vec3 toroidal$nearestDecoration(WorldFold transformer, double xPos, double zPos,
            LocalRef<Vec3> nearest) {
        Vec3 found = nearest.get();
        if (found == null) {
            found = transformer.nearestCopy(new Vec3(this.centerX, 0.0, this.centerZ), new Vec3(xPos, 0.0, zPos));
            nearest.set(found);
        }

        return found;
    }
}
