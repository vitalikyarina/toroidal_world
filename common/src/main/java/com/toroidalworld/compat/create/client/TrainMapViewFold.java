package com.toroidalworld.compat.create.client;

import org.jspecify.annotations.Nullable;

import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.toroidalworld.compat.create.CreateTrackFold;
import com.toroidalworld.compat.create.TrainMapLaps;
import com.toroidalworld.compat.create.TrainMapLaps.Range;
import com.toroidalworld.map.MapSurfaceCopies;
import com.toroidalworld.core.WorldFold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.phys.Vec3;

public final class TrainMapViewFold {
    public record Lap(int offsetX, int offsetZ) {
    }

    public record NearestNodeKey(Vec3i raw, Vec3i nearest) {
    }

    public static @Nullable WorldFold transformer() {
        return TrainMapFrame.current();
    }

    public static NearestNodeKey nearestNodeKey(Vec3i anchor, Vec3i key, LocalRef<NearestNodeKey> memo) {
        NearestNodeKey known = memo.get();
        if (known != null && known.raw() == key) {
            return known;
        }

        WorldFold transformer = transformer();
        NearestNodeKey folded = new NearestNodeKey(key,
                transformer == null ? key : CreateTrackFold.nearestNodeKey(transformer, anchor, key));
        memo.set(folded);
        return folded;
    }

    public static BlockPos wrapPixel(int x, int z) {
        WorldFold transformer = transformer();
        return transformer == null ? new BlockPos(x, 0, z) : wrapPixel(transformer, x, z);
    }

    public static BlockPos wrapPixel(WorldFold transformer, int x, int z) {
        return transformer.fold(new BlockPos(x, 0, z));
    }

    public static Vec3 canonical(Vec3 position) {
        WorldFold transformer = transformer();
        if (transformer == null) {
            return position;
        }

        return transformer.fold(position);
    }

    public static Vec3 nearestTo(Vec3 anchor, Vec3 position) {
        WorldFold transformer = transformer();
        return transformer == null ? position : transformer.nearestCopy(anchor, position);
    }

    public static Lap[] laps(Rect2i bounds) {
        WorldFold transformer = transformer();
        if (transformer == null) {
            return new Lap[] {new Lap(0, 0)};
        }

        MapSurfaceCopies.Copies surface = MapSurfaceCopies.current();
        int surfaceX = surface.rangeX();
        int surfaceZ = surface.rangeZ();
        Range alongX = TrainMapLaps.range(transformer.blockDomain(Direction.Axis.X), bounds.getX(), bounds.getWidth(), surfaceX);
        Range alongZ = TrainMapLaps.range(transformer.blockDomain(Direction.Axis.Z), bounds.getY(), bounds.getHeight(), surfaceZ);
        int worldWidthX = transformer.blockDomain(Direction.Axis.X).domainLength;
        int worldWidthZ = transformer.blockDomain(Direction.Axis.Z).domainLength;
        Lap[] laps = new Lap[alongX.kept() * alongZ.kept()];
        int index = 0;
        for (int lapX = alongX.lowest(); lapX <= alongX.highest(); lapX++) {
            for (int lapZ = alongZ.lowest(); lapZ <= alongZ.highest(); lapZ++) {
                laps[index++] = new Lap(lapX * worldWidthX, lapZ * worldWidthZ);
            }
        }

        return laps;
    }

    private TrainMapViewFold() {
    }
}
