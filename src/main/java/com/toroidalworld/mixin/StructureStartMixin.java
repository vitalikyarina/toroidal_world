package com.toroidalworld.mixin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.toroidalworld.accessors.FramedStructureStart;
import com.toroidalworld.core.CoordinateConstants;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;

// Every chunk of a structure's far half asks for the same thing: this start, moved into their shared frame across the
// seam. Building that by serialising and reloading is not cheap, and doing it per chunk per structure would churn a
// fresh set of pieces for every one of them. It is built once per frame and kept here instead.
//
// On the start, not in a cache of our own: a straddling structure has at most a handful of frames (one per axis it
// crosses), the map is only allocated for the starts that actually need one, and the whole thing is collected with the
// start. A global cache would have to be pruned by hand, and nothing here knows when a start stops mattering.
//
// The copy must be real. The live start serves the near half of the same structure and worldgen runs on several threads,
// so moving its pieces in place would corrupt the half that was already correct. A reloaded start has never had its
// bounding box asked for, so the lazy cache is still empty when the move happens and recomputes against moved pieces.
@Mixin(StructureStart.class)
public class StructureStartMixin implements FramedStructureStart {
    @Unique
    private volatile @Nullable Map<Long, StructureStart> toroidal$framed;

    @Override
    public @Nullable StructureStart toroidal$framedBy(WorldGenLevel level, int deltaChunkX, int deltaChunkZ) {
        StructureStart self = (StructureStart) (Object) this;
        if (deltaChunkX == 0 && deltaChunkZ == 0) {
            return self;
        }

        Map<Long, StructureStart> framed = this.toroidal$cache();
        long frame = ChunkPos.pack(deltaChunkX, deltaChunkZ);
        StructureStart known = framed.get(frame);
        if (known != null) {
            return known;
        }

        StructureStart made = toroidal$movedCopy(self, level, deltaChunkX, deltaChunkZ);
        if (made == null) {
            return null;
        }

        // Two threads may have built the same frame at once; keep whichever landed first so the far half is placed from
        // one set of pieces rather than two equal ones.
        StructureStart raced = framed.putIfAbsent(frame, made);
        return raced != null ? raced : made;
    }

    @Unique
    private Map<Long, StructureStart> toroidal$cache() {
        Map<Long, StructureStart> framed = this.toroidal$framed;
        if (framed != null) {
            return framed;
        }

        synchronized (this) {
            framed = this.toroidal$framed;
            if (framed == null) {
                framed = new ConcurrentHashMap<>(2);
                this.toroidal$framed = framed;
            }

            return framed;
        }
    }

    @Unique
    private static @Nullable StructureStart toroidal$movedCopy(
            StructureStart start, WorldGenLevel level, int deltaChunkX, int deltaChunkZ) {
        StructurePieceSerializationContext context = StructurePieceSerializationContext.fromLevel(level.getLevel());
        StructureStart copy = StructureStart.loadStaticStart(
                context, start.createTag(context, start.getChunkPos()), level.getSeed());
        if (copy == null || !copy.isValid()) {
            return null;
        }

        int blockX = deltaChunkX * CoordinateConstants.CHUNK_WIDTH;
        int blockZ = deltaChunkZ * CoordinateConstants.CHUNK_WIDTH;
        copy.getPieces().forEach(piece -> {
            piece.move(blockX, 0, blockZ);

            // move() shifts the bounding box and position but not the jigsaw junctions, and the Beardifier smooths
            // terrain from junction coordinates — left behind, they would smooth a spot a world away.
            if (piece instanceof PoolElementStructurePiece poolPiece) {
                poolPiece.getJunctions().replaceAll(junction -> new JigsawJunction(
                        junction.getSourceX() + blockX,
                        junction.getSourceGroundY(),
                        junction.getSourceZ() + blockZ,
                        junction.getDeltaY(),
                        junction.getDestProjection()));
            }
        });
        return copy;
    }
}
