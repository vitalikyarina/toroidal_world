package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.core.WrapDomain;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

// The End's terrain is not a continuous noise field the periodic samplers can bend around the world circle: it is a
// grid of island cells (1 chunk / 16 blocks each), and each cell decides whether an island exists there from its raw
// integer coordinates — a distance-from-origin test and a simplex lookup keyed by the cell itself. On a looped End
// both must see the cell's canonical coordinate, or the islands are sliced flat at the seam.
//
// The wrapped path folds every scanned cell (min-image, via the chunk domain) before the distance test, the noise
// lookup and the island-size hash, so a column at the +edge iterates the same physical cell set as the columns that
// actually generate that ground on the -edge.
//
// The cell grid is rebuilt with floor division rather than vanilla's truncating one. Vanilla's negative grid is
// shifted a chunk toward zero (its origin cell spans 31 blocks), so no negative cell lines up with a positive cell
// folded across the seam — the fold only tiles if cells and chunks are the same partition, which floor makes true.
// The cost is islands sitting up to 16 blocks from their vanilla spots on the negative half; a looped End owes its
// layout consistency, not vanilla-identity.
//
// The main-island falloff term stays on vanilla's truncating math on purpose: it only matters within 25 sections
// (200 blocks) of the origin — everywhere near the seam it clamps to -100 on both sides, since the End is at least
// 192 chunks (3072 blocks) wide — and inside 64 chunks (1024 blocks) of the origin no cell passes the distance test,
// so the two regimes never mix and the main island stays exactly vanilla.
@Mixin(targets = "net.minecraft.world.level.levelgen.DensityFunctions$EndIslandDensityFunction")
public class DensityFunctionsEndIslandMixin {
    @Shadow
    @Final
    private SimplexNoise islandNoise;

    // Vanilla-body re-implementation — verified against 26.2; re-diff on a platform bump.
    @WrapMethod(method = "compute(Lnet/minecraft/world/level/levelgen/DensityFunction$FunctionContext;)D")
    private double toroidal$loopedCompute(DensityFunction.FunctionContext context, Operation<Double> original) {
        WorldLoopTransformer transformer = GenerationTransformerContext.context().wrappedTransformer();
        if (transformer == null) {
            return original.call(context);
        }

        return (toroidal$loopedHeightValue(transformer, context.blockX(), context.blockZ()) - 8.0) / 128.0;
    }

    @Unique
    private float toroidal$loopedHeightValue(WorldLoopTransformer transformer, int blockX, int blockZ) {
        int sectionX = blockX / 8;
        int sectionZ = blockZ / 8;
        float doffs = Mth.clamp(100.0F - Mth.sqrt(sectionX * sectionX + sectionZ * sectionZ) * 8.0F, -100.0F, 80.0F);

        int chunkX = Math.floorDiv(blockX, 16);
        int chunkZ = Math.floorDiv(blockZ, 16);
        int subSectionX = Math.floorMod(Math.floorDiv(blockX, 8), 2);
        int subSectionZ = Math.floorMod(Math.floorDiv(blockZ, 8), 2);
        WrapDomain xDomain = transformer.chunks.x;
        WrapDomain zDomain = transformer.chunks.z;

        for (int xo = -12; xo <= 12; xo++) {
            for (int zo = -12; zo <= 12; zo++) {
                long cellX = xDomain.wrap(chunkX + xo);
                long cellZ = zDomain.wrap(chunkZ + zo);
                if (cellX * cellX + cellZ * cellZ > 4096L && this.islandNoise.getValue(cellX, cellZ) < -0.9F) {
                    float islandSize = (Mth.abs((float) cellX) * 3439.0F + Mth.abs((float) cellZ) * 147.0F) % 13.0F + 9.0F;
                    float xd = subSectionX - xo * 2;
                    float zd = subSectionZ - zo * 2;
                    float newDoffs = Mth.clamp(100.0F - Mth.sqrt(xd * xd + zd * zd) * islandSize, -100.0F, 80.0F);
                    doffs = Math.max(doffs, newDoffs);
                }
            }
        }

        return doffs;
    }
}
