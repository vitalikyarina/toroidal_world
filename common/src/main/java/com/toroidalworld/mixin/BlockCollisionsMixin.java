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
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

@Mixin(BlockCollisions.class)
public abstract class BlockCollisionsMixin {
    @Unique
    private @Nullable WorldLoopTransformer toroidal$transformer;

    @Inject(
            method = "<init>(Lnet/minecraft/world/level/CollisionGetter;Lnet/minecraft/world/phys/shapes/CollisionContext;Lnet/minecraft/world/phys/AABB;ZLjava/util/function/BiFunction;)V",
            at = @At("RETURN"))
    private void toroidal$resolveTransformer(CollisionGetter collisionGetter, CollisionContext context, AABB box,
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
