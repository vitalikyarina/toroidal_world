package com.toroidalworld.compat.journeymap;

import java.util.ArrayList;
import java.util.List;

import com.toroidalworld.api.ToroidalShape;
import com.toroidalworld.ToroidalWorld;

import journeymap.api.v2.client.display.PolygonOverlay;
import journeymap.api.v2.client.model.MapPolygon;
import journeymap.api.v2.client.model.ShapeProperties;
import journeymap.api.v2.common.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class SeamOverlays {
    private static final int STROKE_RGB = 0xFFFFFF;
    private static final float STROKE_OPACITY = 0.35f;
    private static final float STROKE_WIDTH = 1.5f;

    public static List<PolygonOverlay> build(ResourceKey<Level> dimension, ToroidalShape shape) {
        if (!shape.loops(Direction.Axis.X) || !shape.loops(Direction.Axis.Z)) {
            return List.of();
        }

        int minX = shape.minBlock(Direction.Axis.X);
        int maxX = shape.maxBlock(Direction.Axis.X);
        int minZ = shape.minBlock(Direction.Axis.Z);
        int maxZ = shape.maxBlock(Direction.Axis.Z);
        int widthX = shape.widthBlocks(Direction.Axis.X);
        int widthZ = shape.widthBlocks(Direction.Axis.Z);

        List<PolygonOverlay> overlays = new ArrayList<>(9);
        for (int lapX = -1; lapX <= 1; lapX++) {
            for (int lapZ = -1; lapZ <= 1; lapZ++) {
                overlays.add(outline(dimension,
                        minX + lapX * widthX, minZ + lapZ * widthZ,
                        maxX + lapX * widthX, maxZ + lapZ * widthZ));
            }
        }

        return overlays;
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
