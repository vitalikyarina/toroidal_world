package com.toroidalworld.compat.journeymap.mixin;

import java.awt.geom.Point2D;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.toroidalworld.compat.journeymap.JourneyMapFold;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

// Keeps the map view consistent with the canonical region files RegionCoordMixin produces. The center a map is
// drawn around comes in as the camera's mirror coordinate — folded here, so the tile grid, the center region and
// the pixel math all live in canonical space. Draw steps (entities, waypoints) hand getBlockPixelInGrid whatever
// space their source used, so those inputs are taken to the copy nearest the folded center.
//
// Two callers deliberately escape the pixel fold. The BlockPos overload serves the renderer's own canvas anchor —
// centerBlock minus half a region, a point that legitimately sits half a world away, which the fold would flip to
// the far side on any fractional center and shove the whole canvas a world off screen ("the map disappears").
// getBlockPixelInGridExact is not folded at all: its callers are the CORNERS of API overlay spans (image/polygon),
// and folding a span's corners point-by-point tears the span the same way.
@Mixin(targets = "journeymap.client.render.map.MapRenderer", remap = false)
public abstract class MapRendererMixin {
    @Shadow(remap = false)
    protected double centerBlockX;

    @Shadow(remap = false)
    protected double centerBlockZ;

    @Shadow(remap = false)
    public abstract void clear();

    // Render-thread only, like every caller of these methods.
    @Unique
    private static boolean toroidal$anchorPass;

    @Unique
    private ResourceKey<Level> toroidal$lastLevelDimension;

    // JourneyMap never evicts tiles on a dimension change — only an explicit clear() removes them — so a tile from
    // the previous dimension keeps rendering at the same region cell as the new dimension's tile, and one such
    // render with the new state poisons the region-image cache with a cross-dimension holder that
    // loadInMemoryRegions (which has no dimension filter) then re-seeds into every freshly cleared grid. Dropping
    // the grid at this funnel — the one place both UIs pass the new dimension through before any render — kills
    // the stale tiles before they can draw, which also starves the poisoning path. A latent JourneyMap defect;
    // our death-respawn bounds sync made it visible and persistent.
    @Inject(method = "center(Ljava/io/File;Ljourneymap/client/model/map/MapType;DDI)Z", at = @At("HEAD"))
    private void toroidal$dropTilesOnDimensionChange(CallbackInfoReturnable<Boolean> cir) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        ResourceKey<Level> dimension = level.dimension();
        if (toroidal$lastLevelDimension != null && toroidal$lastLevelDimension != dimension) {
            this.clear();
            JourneyMapFold.gridDropped(toroidal$lastLevelDimension.identifier().toString(),
                    dimension.identifier().toString());
        }

        toroidal$lastLevelDimension = dimension;
    }

    @ModifyVariable(
            method = "center(Ljava/io/File;Ljourneymap/client/model/map/MapType;DDI)Z",
            at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private double toroidal$foldCenterX(double blockX) {
        return JourneyMapFold.foldCenterCoord(Direction.Axis.X, blockX);
    }

    @ModifyVariable(
            method = "center(Ljava/io/File;Ljourneymap/client/model/map/MapType;DDI)Z",
            at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private double toroidal$foldCenterZ(double blockZ) {
        return JourneyMapFold.foldCenterCoord(Direction.Axis.Z, blockZ);
    }

    @Inject(method = "getBlockPixelInGrid(Lnet/minecraft/core/BlockPos;)Ljava/awt/geom/Point2D$Double;",
            at = @At("HEAD"))
    private void toroidal$beginAnchorPass(CallbackInfoReturnable<Point2D.Double> cir) {
        toroidal$anchorPass = true;
    }

    @Inject(method = "getBlockPixelInGrid(Lnet/minecraft/core/BlockPos;)Ljava/awt/geom/Point2D$Double;",
            at = @At("RETURN"))
    private void toroidal$endAnchorPass(CallbackInfoReturnable<Point2D.Double> cir) {
        toroidal$anchorPass = false;
    }

    @ModifyVariable(method = "getBlockPixelInGrid(DD)Ljava/awt/geom/Point2D$Double;",
            at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private double toroidal$foldPixelX(double blockX) {
        return toroidal$anchorPass ? blockX
                : JourneyMapFold.nearestPixelCoord(Direction.Axis.X, this.centerBlockX, blockX);
    }

    @ModifyVariable(method = "getBlockPixelInGrid(DD)Ljava/awt/geom/Point2D$Double;",
            at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private double toroidal$foldPixelZ(double blockZ) {
        return toroidal$anchorPass ? blockZ
                : JourneyMapFold.nearestPixelCoord(Direction.Axis.Z, this.centerBlockZ, blockZ);
    }

    // The tile ring must never shrink below the whole canonical world: the glued copies render from these tiles,
    // so a far-side region leaving the ring takes its copies with it.
    @Inject(method = "getCalculatedGridSize(I)I", at = @At("RETURN"), cancellable = true)
    private void toroidal$floorGridSizeToWorld(int zoom, CallbackInfoReturnable<Integer> cir) {
        int floor = JourneyMapFold.minGridSize();
        if (floor > cir.getReturnValue()) {
            cir.setReturnValue(floor);
        }
    }
}
