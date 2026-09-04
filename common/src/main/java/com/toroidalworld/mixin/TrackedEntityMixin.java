package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.InjectionTargets;
import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldFold;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

@Mixin(targets = "net/minecraft/server/level/ChunkMap$TrackedEntity")
public class TrackedEntityMixin {
    @Shadow
    @Final
    Entity entity;

    @WrapOperation(
            method = "updatePlayer",
            at = @At(
                    value = "INVOKE",
                    target = InjectionTargets.VEC3_SUBTRACT))
    private Vec3 toroidal$deltaThroughSeam(Vec3 playerPosition, Vec3 entityPosition, Operation<Vec3> original) {
        WorldFold transformer = ((TransformerSource) this.entity).toroidal$wrappedTransformer();
        if (transformer == null) {
            return original.call(playerPosition, entityPosition);
        }

        return original.call(transformer.nearestCopy(entityPosition, playerPosition), entityPosition);
    }

    @WrapOperation(
            method = "updatePlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ChunkMap;isChunkTracked(Lnet/minecraft/server/level/ServerPlayer;II)Z"))
    private boolean toroidal$trackPhysicalChunk(ChunkMap chunkMap, ServerPlayer player, int chunkX, int chunkZ,
            Operation<Boolean> original) {
        WorldFold transformer = ((TransformerSource) this.entity).toroidal$wrappedTransformer();
        if (transformer == null) {
            return original.call(chunkMap, player, chunkX, chunkZ);
        }

        long folded = transformer.foldChunkKey(ChunkPos.pack(chunkX, chunkZ));
        return original.call(chunkMap, player, ChunkPos.getX(folded), ChunkPos.getZ(folded));
    }
}
