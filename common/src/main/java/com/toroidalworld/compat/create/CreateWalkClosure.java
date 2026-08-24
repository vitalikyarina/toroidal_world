package com.toroidalworld.compat.create;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.LevelAccessor;

public final class CreateWalkClosure {
    private final WorldLoopTransformer transformer;

    private @Nullable BlockPos legStart;
    private @Nullable BlockPos lastQuery;
    private @Nullable Vec3i step;
    private @Nullable BlockPos origin;

    public static @Nullable CreateWalkClosure of(LevelAccessor world) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOfReader(world);
        return transformer == null ? null : new CreateWalkClosure(transformer);
    }

    CreateWalkClosure(WorldLoopTransformer transformer) {
        this.transformer = transformer;
    }

    public boolean closes(BlockPos query) {
        if (lastQuery != null && continues(query, lastQuery)) {
            lastQuery = query;
            BlockPos legOrigin = origin;
            return legOrigin != null && !query.equals(legOrigin)
                    && transformer.blocks.wrap(query).equals(transformer.blocks.wrap(legOrigin));
        }

        openLeg(query);
        return false;
    }

    private boolean continues(BlockPos query, BlockPos previous) {
        Vec3i delta = query.subtract(previous);
        Vec3i legStep = step;
        if (legStep != null) {
            return delta.equals(legStep);
        }

        BlockPos first = legStart;
        if (first == null || !isUnitStep(delta)) {
            return false;
        }

        step = delta;
        origin = first.subtract(delta);
        return true;
    }

    private void openLeg(BlockPos query) {
        legStart = query;
        lastQuery = query;
        step = null;
        origin = null;
    }

    private static boolean isUnitStep(Vec3i delta) {
        return Math.abs(delta.getX()) + Math.abs(delta.getY()) + Math.abs(delta.getZ()) == 1;
    }
}
