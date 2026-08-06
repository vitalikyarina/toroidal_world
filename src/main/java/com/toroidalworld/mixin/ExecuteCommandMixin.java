package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.server.commands.ExecuteCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

// `execute if blocks` is a question, and a question has to be answered. Refusing an ambiguous region the way /fill
// does would throw out of the middle of a datapack's condition chain — the whole command fails instead of the check
// simply saying "no" — so this one takes the shorter of the two readings instead.
//
// Only the source region is folded. The destination is built right after, from this region's own length, so it
// follows along; the comparison walk then reads both through the wrapped Level.getChunk like any other block access.
@Mixin(ExecuteCommand.class)
public class ExecuteCommandMixin {
    @ModifyExpressionValue(
            method = "checkRegions(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Z)Ljava/util/OptionalInt;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/structure/BoundingBox;fromCorners(Lnet/minecraft/core/Vec3i;Lnet/minecraft/core/Vec3i;)Lnet/minecraft/world/level/levelgen/structure/BoundingBox;",
                    ordinal = 0))
    private static BoundingBox toroidal$foldComparedRegion(BoundingBox region,
            @Local(argsOnly = true) ServerLevel level) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        return transformer == null ? region : transformer.foldAcrossSeam(region);
    }
}
