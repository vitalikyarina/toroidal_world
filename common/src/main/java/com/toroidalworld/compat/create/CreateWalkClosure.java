package com.toroidalworld.compat.create;

import org.jspecify.annotations.Nullable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class CreateWalkClosure {
    private final WorldFold transformer;

    private @Nullable BlockPos legStart;
    private @Nullable BlockPos lastQuery;
    private @Nullable Vec3i step;
    private @Nullable BlockPos origin;

    public static @Nullable CreateWalkClosure of(LevelAccessor world) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOfReader(world);
        return transformer == null ? null : new CreateWalkClosure(transformer);
    }

    public static BlockState read(LevelAccessor world, BlockPos pos, Operation<BlockState> original,
            LocalRef<CreateWalkClosure> closureRef, LocalBooleanRef resolved) {
        if (!resolved.get()) {
            closureRef.set(of(world));
            resolved.set(true);
        }

        CreateWalkClosure closure = closureRef.get();
        if (closure == null || !closure.closes(pos)) {
            return original.call(world, pos);
        }

        return Blocks.AIR.defaultBlockState();
    }

    CreateWalkClosure(WorldFold transformer) {
        this.transformer = transformer;
    }

    public boolean closes(BlockPos query) {
        if (lastQuery != null && continues(query, lastQuery)) {
            lastQuery = query;
            BlockPos legOrigin = origin;
            return legOrigin != null && !query.equals(legOrigin)
                    && transformer.fold(query).equals(transformer.fold(legOrigin));
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
