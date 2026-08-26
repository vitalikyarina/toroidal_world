package com.toroidalworld.compat.journeymap;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.toroidalworld.api.ToroidalShape;
import com.toroidalworld.compat.AxisCopies;
import com.toroidalworld.ToroidalWorld;
import com.mojang.logging.LogUtils;

import journeymap.api.v2.client.display.PolygonOverlay;
import journeymap.api.v2.client.model.MapPolygon;
import journeymap.api.v2.client.model.ShapeProperties;
import journeymap.api.v2.client.display.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class SeamOverlays {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int STROKE_RGB = 0xFFFFFF;
    private static final float STROKE_OPACITY = 0.35f;
    private static final float STROKE_WIDTH = 1.5f;

    private static final int TORUS_OVERLAYS = 9;

    public static List<PolygonOverlay> build(ResourceKey<Level> dimension, ToroidalShape shape) {
        AxisCopies x = AxisCopies.of(shape, Direction.Axis.X);
        AxisCopies z = AxisCopies.of(shape, Direction.Axis.Z);
        List<PolygonOverlay> overlays = new ArrayList<>(x.laps().size() * z.laps().size());
        for (int lapX : x.laps()) {
            for (int lapZ : z.laps()) {
                overlays.add(outline(dimension, lower(x, lapX), lower(z, lapZ), upper(x, lapX), upper(z, lapZ)));
            }
        }

        LOGGER.info("[jm-compat] seam_overlays dim={} {} {} overlays={} legacy_overlays={}",
                dimension.location(), describe("x", x), describe("z", z), overlays.size(),
                x.loops() && z.loops() ? TORUS_OVERLAYS : 0);
        return overlays;
    }

    private static int lower(AxisCopies axis, int lap) {
        return axis.loops() ? axis.min() + axis.offset(lap) : -Level.MAX_LEVEL_SIZE;
    }

    private static int upper(AxisCopies axis, int lap) {
        return axis.loops() ? axis.max() + axis.offset(lap) : Level.MAX_LEVEL_SIZE;
    }

    private static String describe(String axis, AxisCopies copies) {
        if (!copies.loops()) {
            return axis + "_loops=false " + axis + "_laps=" + copies.laps().size();
        }

        return axis + "_loops=true " + axis + "_laps=" + copies.laps().size()
                + " " + axis + "_min=" + copies.min() + " " + axis + "_width_blocks=" + copies.width();
    }

    private static PolygonOverlay outline(ResourceKey<Level> dimension, int minX, int minZ, int maxX, int maxZ) {
        ShapeProperties properties = new ShapeProperties()
                .setStrokeColor(STROKE_RGB)
                .setStrokeOpacity(STROKE_OPACITY)
                .setStrokeWidth(STROKE_WIDTH)
                .setFillOpacity(0.0f);
        MapPolygon rectangle = new MapPolygon(
                new BlockPos(minX, 0, minZ),
                new BlockPos(minX, 0, maxZ),
                new BlockPos(maxX, 0, maxZ),
                new BlockPos(maxX, 0, minZ));

        PolygonOverlay overlay = new PolygonOverlay(ToroidalWorld.MODID, dimension, properties, rectangle);
        overlay.setOverlayGroupName("World seam");
        overlay.setActiveUIs(Context.UI.Fullscreen);
        return overlay;
    }

    private SeamOverlays() {
    }
}
