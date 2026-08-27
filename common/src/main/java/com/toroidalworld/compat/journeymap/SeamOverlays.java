package com.toroidalworld.compat.journeymap;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.toroidalworld.api.ToroidalShape;
import com.toroidalworld.compat.AxisCopies;
import com.toroidalworld.compat.FullscreenZoomFloor;
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

    private static final int COVERED_WINDOW_PIXELS = 8192;
    private static final int SEAM_LAPS_EACH_SIDE = COVERED_WINDOW_PIXELS / FullscreenZoomFloor.MIN_WORLD_PIXELS / 2;

    public static List<PolygonOverlay> build(ResourceKey<Level> dimension, ToroidalShape shape) {
        AxisCopies x = AxisCopies.of(shape, Direction.Axis.X);
        AxisCopies z = AxisCopies.of(shape, Direction.Axis.Z);
        int[] spanX = span(x);
        int[] spanZ = span(z);
        int[] seamsX = x.seams(spanX[0], spanX[1]);
        int[] seamsZ = z.seams(spanZ[0], spanZ[1]);
        List<PolygonOverlay> overlays = new ArrayList<>(seamsX.length + seamsZ.length);
        for (int seam : seamsX) {
            overlays.add(line(dimension, seam, spanZ[0], seam + 1, spanZ[1]));
        }

        for (int seam : seamsZ) {
            overlays.add(line(dimension, spanX[0], seam, spanX[1], seam + 1));
        }

        LOGGER.info("[jm-compat] seam_overlays dim={} {} {} overlays={}",
                dimension.location(), describe("x", x, seamsX), describe("z", z, seamsZ), overlays.size());
        return overlays;
    }

    private static int[] span(AxisCopies copies) {
        if (!copies.loops()) {
            return new int[] {-Level.MAX_LEVEL_SIZE, Level.MAX_LEVEL_SIZE};
        }

        int reach = SEAM_LAPS_EACH_SIDE * copies.width();
        return new int[] {copies.min() - reach, copies.max() + reach};
    }

    private static String describe(String axis, AxisCopies copies, int[] seams) {
        if (!copies.loops()) {
            return axis + "_loops=false";
        }

        return axis + "_loops=true " + axis + "_seams=" + seams.length
                + " " + axis + "_min=" + copies.min() + " " + axis + "_width_blocks=" + copies.width();
    }

    private static PolygonOverlay line(ResourceKey<Level> dimension, int minX, int minZ, int maxX, int maxZ) {
        ShapeProperties properties = new ShapeProperties()
                .setStrokeColor(STROKE_RGB)
                .setStrokeOpacity(STROKE_OPACITY)
                .setStrokeWidth(STROKE_WIDTH)
                .setFillOpacity(0.0f);
        MapPolygon strip = new MapPolygon(
                new BlockPos(minX, 0, minZ),
                new BlockPos(minX, 0, maxZ),
                new BlockPos(maxX, 0, maxZ),
                new BlockPos(maxX, 0, minZ));

        PolygonOverlay overlay = new PolygonOverlay(ToroidalWorld.MODID, dimension, properties, strip);
        overlay.setOverlayGroupName("World seam");
        overlay.setActiveUIs(Context.UI.Fullscreen);
        return overlay;
    }

    private SeamOverlays() {
    }
}
