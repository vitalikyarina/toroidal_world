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

// A map turns every world position into a canvas delta from its stored center, and the delta is taken raw: a player
// arrow, banner, frame or explorer target across the seam reads a world apart from ground that physically sits beside
// the mapped area, so it lands clamped on the wrong edge as off-map or is dropped. Every incoming position is folded
// to its copy nearest the center before the delta is taken; the canonical position stays with the caller — banners
// and frames persist their real coordinates, only the canvas arithmetic sees the folded one. Seam-straddling maps are
// the norm: the vanilla map grid is anchored at -64 and never aligns with the world bounds, and a center snapped past
// them is a valid reference — unwrapAround folds around an out-of-bounds reference correctly.
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

    // The banner's ±63-pixel range check runs on the same raw delta before addDecoration is ever reached, so a banner
    // physically inside the map's area through the seam is refused outright. The banner's world position is folded at
    // its store; the BlockPos the banner is read and keyed by stays raw, so the persisted marker keeps its real
    // coordinates and the fold in addDecoration is an identity on the already-folded value.
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
