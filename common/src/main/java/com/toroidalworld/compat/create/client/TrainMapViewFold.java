package com.toroidalworld.compat.create.client;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.toroidalworld.compat.create.CreateTrackFold;
import com.toroidalworld.compat.create.TrainMapLaps;
import com.toroidalworld.compat.create.TrainMapLaps.Range;
import com.toroidalworld.compat.create.CreateTrackFold.NodeKeyAxes;
import com.toroidalworld.core.LogRateGate;
import com.toroidalworld.map.MapSurfaceCopies;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.core.WrapDomain;

import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.phys.Vec3;

public final class TrainMapViewFold {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final LogRateGate capGate = new LogRateGate();

    public record Lap(int offsetX, int offsetZ) {
    }

    public static @Nullable WorldLoopTransformer transformer() {
        return TrainMapFrame.current();
    }

    public static int foldNodeKeyX(TrackNodeLocation anchor, int rawCoord) {
        WorldLoopTransformer transformer = transformer();
        if (transformer == null) {
            return rawCoord;
        }

        return foldNodeKey(nodeKeyDomainX(transformer), anchor.getX(), rawCoord);
    }

    public static int foldNodeKeyZ(TrackNodeLocation anchor, int rawCoord) {
        WorldLoopTransformer transformer = transformer();
        if (transformer == null) {
            return rawCoord;
        }

        return foldNodeKey(nodeKeyDomainZ(transformer), anchor.getZ(), rawCoord);
    }

    public static int wrapPixelX(int coord) {
        WorldLoopTransformer transformer = transformer();
        return transformer == null ? coord : transformer.coords.x.wrap(coord);
    }

    public static int wrapPixelZ(int coord) {
        WorldLoopTransformer transformer = transformer();
        return transformer == null ? coord : transformer.coords.z.wrap(coord);
    }

    public static Vec3 canonical(Vec3 position) {
        WorldLoopTransformer transformer = transformer();
        if (transformer == null) {
            return position;
        }

        return transformer.vectors.wrap(position);
    }

    public static Vec3 nearestTo(Vec3 anchor, Vec3 position) {
        WorldLoopTransformer transformer = transformer();
        return transformer == null ? position : transformer.vectors.nearestCopy(anchor, position);
    }

    public static Lap[] laps(Rect2i bounds) {
        WorldLoopTransformer transformer = transformer();
        if (transformer == null) {
            return new Lap[] {new Lap(0, 0)};
        }

        MapSurfaceCopies.Copies surface = MapSurfaceCopies.current();
        int surfaceX = surface.rangeX();
        int surfaceZ = surface.rangeZ();
        Range alongX = TrainMapLaps.range(transformer.coords.x, bounds.getX(), bounds.getWidth(), surfaceX);
        Range alongZ = TrainMapLaps.range(transformer.coords.z, bounds.getY(), bounds.getHeight(), surfaceZ);
        int worldWidthX = transformer.coords.x.domainLength;
        int worldWidthZ = transformer.coords.z.domainLength;
        Lap[] laps = new Lap[alongX.kept() * alongZ.kept()];
        int index = 0;
        for (int lapX = alongX.lowest(); lapX <= alongX.highest(); lapX++) {
            for (int lapZ = alongZ.lowest(); lapZ <= alongZ.highest(); lapZ++) {
                laps[index++] = new Lap(lapX * worldWidthX, lapZ * worldWidthZ);
            }
        }

        if ((alongX.capped() || alongZ.capped()) && capGate.tryPass()) {
            LOGGER.info("[trainmap] laps_capped needed_x={} kept_x={} needed_z={} kept_z={} world_width_x={}"
                    + " view_width={} surface_x={} surface_z={} units=copies",
                    alongX.needed(), alongX.kept(), alongZ.needed(), alongZ.kept(), worldWidthX,
                    bounds.getWidth(), surfaceX, surfaceZ);
        }


        return laps;
    }

    private static int foldNodeKey(WrapDomain domain, int anchor, int rawCoord) {
        return anchor + domain.foldDelta(rawCoord - anchor);
    }

    private static WrapDomain nodeKeyDomainX(WorldLoopTransformer transformer) {
        return axes(transformer).x();
    }

    private static WrapDomain nodeKeyDomainZ(WorldLoopTransformer transformer) {
        return axes(transformer).z();
    }

    private static NodeKeyAxes axes(WorldLoopTransformer transformer) {
        return CreateTrackFold.nodeKeyAxes(transformer);
    }

    private TrainMapViewFold() {
    }
}
