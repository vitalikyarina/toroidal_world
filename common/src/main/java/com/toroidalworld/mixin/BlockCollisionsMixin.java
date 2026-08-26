package com.toroidalworld.mixin;

import java.util.function.BiFunction;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

@Mixin(BlockCollisions.class)
public abstract class BlockCollisionsMixin {
    @Unique
    private @Nullable WorldFold toroidal$transformer;

    @Inject(
            method = "<init>(Lnet/minecraft/world/level/CollisionGetter;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;ZLjava/util/function/BiFunction;)V",
            at = @At("RETURN"))
    private void toroidal$resolveTransformer(CollisionGetter collisionGetter, Entity entity, AABB box,
            boolean onlySuffocatingBlocks, BiFunction<BlockPos.MutableBlockPos, VoxelShape, ?> resultProvider,
            CallbackInfo ci) {
        this.toroidal$transformer = collisionGetter instanceof Level level
                ? WorldLoopAttachments.wrappedTransformerOf(level)
                : null;
    }

    @WrapOperation(
            method = "computeNext",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/BlockCollisions;getChunk(II)Lnet/minecraft/world/level/BlockGetter;"))
    private BlockGetter toroidal$chunkThroughSeam(BlockCollisions<?> self, int x, int z, Operation<BlockGetter> original) {
        WorldFold transformer = this.toroidal$transformer;
        if (transformer == null) {
            return original.call(self, x, z);
        }

        long folded = transformer.foldBlockNode(BlockPos.asLong(x, 0, z));
        return original.call(self, BlockPos.getX(folded), BlockPos.getZ(folded));
    }
}
