package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.core.SeamSpans;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.ForceLoadCommand;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

@Mixin(ForceLoadCommand.class)
public class ForceLoadCommandMixin {
    @WrapMethod(method = "changeForceLoad")
    private static int toroidal$foldRequestedRange(CommandSourceStack source, ColumnPos from, ColumnPos to,
            boolean add, Operation<Integer> original) throws CommandSyntaxException {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(source.getLevel());
        if (transformer == null) {
            return original.call(source, from, to, add);
        }

        BoundingBox folded = SeamSpans.foldAcrossSeam(transformer, new BoundingBox(
                Math.min(from.x(), to.x()), 0, Math.min(from.z(), to.z()),
                Math.max(from.x(), to.x()), 0, Math.max(from.z(), to.z())));
        return original.call(
                source,
                new ColumnPos(folded.minX(), folded.minZ()),
                new ColumnPos(folded.maxX(), folded.maxZ()),
                add);
    }

    @WrapOperation(
            method = "changeForceLoad",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setChunkForced(IIZ)Z"))
    private static boolean toroidal$reserveThePhysicalChunk(ServerLevel level, int chunkX, int chunkZ, boolean forced,
            Operation<Boolean> original) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        if (transformer == null) {
            return original.call(level, chunkX, chunkZ, forced);
        }

        long folded = transformer.foldChunkKey(ChunkPos.pack(chunkX, chunkZ));
        return original.call(level, ChunkPos.getX(folded), ChunkPos.getZ(folded), forced);
    }

    @ModifyExpressionValue(
            method = "changeForceLoad",
            at = @At(value = "NEW", target = "net/minecraft/world/level/ChunkPos"))
    private static ChunkPos toroidal$reportThePhysicalChunk(ChunkPos chunkPos,
            @Local(argsOnly = true) CommandSourceStack source) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(source.getLevel());
        return transformer == null ? chunkPos : transformer.fold(chunkPos);
    }
}
