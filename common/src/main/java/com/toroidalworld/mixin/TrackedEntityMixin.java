package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldLoopTransformer;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(targets = "net/minecraft/server/level/ChunkMap$TrackedEntity")
public class TrackedEntityMixin {
    @Shadow
    @Final
    Entity entity;

    @ModifyExpressionValue(
            method = "updatePlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;subtract(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$deltaThroughSeam(Vec3 delta, @Local(argsOnly = true) ServerPlayer player) {
        WorldLoopTransformer transformer = ((TransformerSource) this.entity).toroidal$wrappedTransformer();
        if (transformer == null) {
            return delta;
        }

        return new Vec3(
                transformer.coords.x.deltaFromBounds(this.entity.getX(), player.getX()),
                delta.y,
                transformer.coords.z.deltaFromBounds(this.entity.getZ(), player.getZ()));
    }

    @WrapOperation(
            method = "updatePlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ChunkMap;isChunkTracked(Lnet/minecraft/server/level/ServerPlayer;II)Z"))
    private boolean toroidal$trackPhysicalChunk(ChunkMap chunkMap, ServerPlayer player, int chunkX, int chunkZ,
            Operation<Boolean> original) {
        WorldLoopTransformer transformer = ((TransformerSource) this.entity).toroidal$wrappedTransformer();
        if (transformer == null) {
            return original.call(chunkMap, player, chunkX, chunkZ);
        }

        return original.call(chunkMap, player, transformer.chunks.x.wrap(chunkX), transformer.chunks.z.wrap(chunkZ));
    }
}
