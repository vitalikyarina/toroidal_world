package com.toroidalworld.noise;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.core.WrapDomain;

import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

// The End's island field, folded — stated once for the two mixins that have to state it: the vanilla-shaped
// DensityFunctionsEndIslandMixin and its C2ME-shaped twin, which speak to the same method under two different owners.
//
// The End is not a continuous noise field the periodic samplers can bend around the world circle: it is a grid of
// island cells (1 chunk / 16 blocks each), and each cell decides whether an island exists there from its raw integer
// coordinates — a distance-from-origin test and a simplex lookup keyed by the cell itself. On a looped End both must
// see the cell's canonical coordinate, or the islands are sliced flat at the seam.
//
// Every scanned cell is folded (min-image, via the chunk domain) before the distance test, the noise lookup and the
// island-size hash, so a column at the +edge iterates the same physical cell set as the columns that actually generate
// that ground on the -edge.
//
// The cell grid is rebuilt with floor division rather than vanilla's truncating one. Vanilla's negative grid is
// shifted a chunk toward zero (its origin cell spans 31 blocks), so no negative cell lines up with a positive cell
// folded across the seam — the fold only tiles if cells and chunks are the same partition, which floor makes true.
// The cost is islands sitting up to 16 blocks from their vanilla spots on the negative half; a looped End owes its
// layout consistency, not vanilla-identity.
//
// The main-island falloff term keeps vanilla's truncating math and folds its coordinate instead. Inside the world the
// fold is the identity, so the main island stays exactly vanilla; a query a lap out arrives at its canonical copy
// rather than at the -100 floor, which is the whole difference between a periodic field and one that reads as void
// everywhere but the first lap. Truncating and folding compose because the world's width is a whole number of the
// 8-block sections the term counts in.
public final class PeriodicEndIslands {
    // Vanilla-body re-implementation — verified against 26.2; re-diff on a platform bump.
    public static float heightValue(SimplexNoise islandNoise, WorldLoopTransformer transformer, int blockX, int blockZ) {
        int sectionX = transformer.coords.x.wrap(blockX) / 8;
        int sectionZ = transformer.coords.z.wrap(blockZ) / 8;
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
                if (cellX * cellX + cellZ * cellZ > 4096L && islandNoise.getValue(cellX, cellZ) < -0.9F) {
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

    // What the folded height value is worth as a density: vanilla's own conversion, kept beside the walk so the two
    // callers cannot drift on it.
    public static double density(float heightValue) {
        return (heightValue - 8.0) / 128.0;
    }

    private PeriodicEndIslands() {
    }
}
