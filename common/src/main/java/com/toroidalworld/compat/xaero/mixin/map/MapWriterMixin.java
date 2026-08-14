package com.toroidalworld.compat.xaero.mixin.map;

import java.util.ArrayDeque;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import com.toroidalworld.compat.xaero.XaeroWorldMapFold;

import net.minecraft.core.Direction;

import xaero.map.MapProcessor;
import xaero.map.MapWriter;
import xaero.map.region.LeveledRegion;
import xaero.map.region.MapRegion;

// The world map's persistence is keyed by the client's mirror coordinates, which run a world width per lap — the
// stored map would grow a fresh strip forever. Two cuts fix the write side:
//
// 1. The writeChunk dispatch carries the storage keys (tile chunk + its in-region slot) and the level-access keys
//    (chunk coords) as separate arguments — the storage keys fold canonical, the level reads stay in mirror space
//    where the client's chunks actually are. The in-region slot is recomputed from the folded tile chunk rather
//    than folded itself: a nether world is narrower than a region, so the slot is not fold-invariant.
// 2. The region visit/load tail enumerates regions straight off the mirror window, and regions cannot be folded
//    as indices (the canonical world covers parts of two regions per axis) — the region fetch inside the loop is
//    redirected to drain a queue of the canonical regions the folded window covers. When the window holds fewer
//    slots than the queue, the rest carries over to the next frame — onRender runs per frame, so coverage
//    completes within a few frames. Without this the writer would wait forever on canonical regions nothing
//    requests the loading of. The tail is redirected rather than cancelled: the whole method body sits inside
//    synchronized(renderThreadPauseSync), and a cancellation would return past the monitor exit
//    (IllegalMonitorStateException — found the hard way).
@Mixin(value = MapWriter.class, remap = false)
public abstract class MapWriterMixin {
    @Shadow
    private int startTileChunkX;
    @Shadow
    private int startTileChunkZ;
    @Shadow
    private int endTileChunkX;
    @Shadow
    private int endTileChunkZ;

    @Unique
    private final ArrayDeque<int[]> toroidal$visitQueue = new ArrayDeque<>();

    @ModifyArgs(
            method = "writeMap",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/MapWriter;writeChunk(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/Registry;IZLnet/minecraft/core/Registry;Lxaero/map/region/OverlayManager;ZZZZZLnet/minecraft/core/BlockPos$MutableBlockPos;Lxaero/map/biome/BlockTintProvider;IIIIIIIIILxaero/map/region/MapUpdateFastConfig;)Z"))
    private void toroidal$foldWriteKeys(Args args) {
        int tileChunkX = args.get(16);
        int tileChunkZ = args.get(17);
        int foldedX = XaeroWorldMapFold.foldTileChunk(Direction.Axis.X, tileChunkX);
        int foldedZ = XaeroWorldMapFold.foldTileChunk(Direction.Axis.Z, tileChunkZ);
        if (foldedX == tileChunkX && foldedZ == tileChunkZ) {
            return;
        }

        args.set(16, foldedX);
        args.set(17, foldedZ);
        args.set(18, foldedX & 7);
        args.set(19, foldedZ & 7);
    }

    @Redirect(
            method = "onRender",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/MapProcessor;getLeafMapRegion(IIIZ)Lxaero/map/region/MapRegion;"))
    private MapRegion toroidal$visitCanonicalRegion(MapProcessor processor, int caveLayer, int regionX, int regionZ, boolean create) {
        if (!XaeroWorldMapFold.active()) {
            return processor.getLeafMapRegion(caveLayer, regionX, regionZ, create);
        }

        if (this.toroidal$visitQueue.isEmpty()) {
            int[] regionsX = XaeroWorldMapFold.canonicalRegions(Direction.Axis.X, this.startTileChunkX, this.endTileChunkX);
            int[] regionsZ = XaeroWorldMapFold.canonicalRegions(Direction.Axis.Z, this.startTileChunkZ, this.endTileChunkZ);
            for (int canonicalRegionX : regionsX) {
                for (int canonicalRegionZ : regionsZ) {
                    this.toroidal$visitQueue.add(new int[] {canonicalRegionX, canonicalRegionZ});
                }
            }
        }

        int[] next = this.toroidal$visitQueue.poll();
        return processor.getLeafMapRegion(caveLayer, next[0], next[1], true);
    }

    @Redirect(
            method = "onRender",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/region/LeveledRegion;setComparison(IIIII)V"))
    private void toroidal$foldComparison(int x, int z, int level, int leafX, int leafZ) {
        if (!XaeroWorldMapFold.active()) {
            LeveledRegion.setComparison(x, z, level, leafX, leafZ);
            return;
        }

        int canonicalX = XaeroWorldMapFold.foldChunk(Direction.Axis.X, x + 16) - 16;
        int canonicalZ = XaeroWorldMapFold.foldChunk(Direction.Axis.Z, z + 16) - 16;
        LeveledRegion.setComparison(canonicalX, canonicalZ, level, canonicalX, canonicalZ);
    }
}
