package com.toroidalworld.compat.create.client;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.toroidalworld.compat.create.CreateTrackFold;
import com.toroidalworld.core.DeckTransformation;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.map.MapSurfaceCopies;
import com.toroidalworld.map.MapSurfaceCopies.Copies;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;

public final class TrainMapViewFold {
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

    public static List<DeckTransformation> copies(Rect2i view) {
        WorldFold transformer = transformer();
        if (transformer == null) {
            return List.of(DeckTransformation.IDENTITY);
        }

        return copies(transformer, MapSurfaceCopies.current(), view);
    }

    public static List<DeckTransformation> copies(WorldFold transformer, Copies surface, Rect2i view) {
        BoundingBox painted = surface.painted();
        int minX = Math.max(view.getX(), painted.minX());
        int maxX = Math.min(view.getX() + view.getWidth() - 1, painted.maxX());
        int minZ = Math.max(view.getY(), painted.minZ());
        int maxZ = Math.min(view.getY() + view.getHeight() - 1, painted.maxZ());
        if (minX > maxX || minZ > maxZ) {
            return List.of();
        }

        return transformer.copiesTouching(new BoundingBox(minX, 0, minZ, maxX, 0, maxZ), surface.reach());
    }

    public static Rect2i canonicalView(DeckTransformation copy, Rect2i view) {
        BoundingBox canonical = toCanonical(copy).apply(new BoundingBox(view.getX(), 0, view.getY(),
                view.getX() + view.getWidth() - 1, 0, view.getY() + view.getHeight() - 1));
        return new Rect2i(canonical.minX(), canonical.minZ(), canonical.getXSpan(), canonical.getZSpan());
    }

    public static BlockPos canonicalPixel(DeckTransformation copy, int x, int z) {
        return toCanonical(copy).apply(new BlockPos(x, 0, z));
    }

    private static DeckTransformation toCanonical(DeckTransformation copy) {
        return new DeckTransformation(copy.blocks().inverse());
    }

    private TrainMapViewFold() {
    }
}
