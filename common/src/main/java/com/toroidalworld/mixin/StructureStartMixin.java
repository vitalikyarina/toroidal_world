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
