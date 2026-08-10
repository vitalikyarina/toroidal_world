package com.toroidalworld.mixin;

import java.util.function.BiFunction;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

// Block collision walks the cells the entity's box overlaps and reads each from the level. Past the seam those cells are
// empty space, so a mob pressed against the boundary has no floor to step onto and no wall to stop it — it stalls at the
// edge and never crosses under its own physics. The world is finite but chunk-aligned, so the copy of a cell across the
// seam is the same cell modulo the world width and, the width being a whole number of chunks, it sits in the same slot
// within its chunk. Fetching the wrapped chunk therefore returns the real block while the cell coordinate stays in the
// entity's own frame — the low bits still index correctly — so every collision result lands where the movement maths
// expects it. The mob steps over the boundary onto the real ground that continues there, and the end-of-tick wrap
// normalises its position with no teleport of its own.
@Mixin(BlockCollisions.class)
public abstract class BlockCollisionsMixin {
    @Unique
    private @Nullable WorldLoopTransformer toroidal$transformer;

    @Inject(
            method = "<init>(Lnet/minecraft/world/level/CollisionGetter;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;ZLjava/util/function/BiFunction;)V",
            at = @At("RETURN"))
    private void toroidal$resolveTransformer(CollisionGetter collisionGetter, Entity entity, AABB box,
            boolean onlySuffocatingBlocks, BiFunction<BlockPos.MutableBlockPos, VoxelShape, ?> resultProvider,
            CallbackInfo ci) {
        this.toroidal$transformer = collisionGetter instanceof ServerLevel level
                ? WorldLoopAttachments.wrappedTransformerOf(level)
                : null;
    }

    @WrapOperation(
            method = "computeNext",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/BlockCollisions;getChunk(II)Lnet/minecraft/world/level/BlockGetter;"))
    private BlockGetter toroidal$chunkThroughSeam(BlockCollisions<?> self, int x, int z, Operation<BlockGetter> original) {
        WorldLoopTransformer transformer = this.toroidal$transformer;
        return transformer == null
                ? original.call(self, x, z)
                : original.call(self, transformer.coords.x.wrap(x), transformer.coords.z.wrap(z));
    }
}
