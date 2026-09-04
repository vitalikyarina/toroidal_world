package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.InjectionTargets;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.commands.LocateCommand;

@Mixin(LocateCommand.class)
public class LocateCommandMixin {
    @Unique
    private static final String SHOW_LOCATE_RESULT =
            "showLocateResult(Lnet/minecraft/commands/CommandSourceStack;Lnet/minecraft/core/BlockPos;Lcom/mojang/datafixers/util/Pair;Ljava/lang/String;ZLjava/lang/String;Ljava/time/Duration;)I";

    @WrapOperation(
            method = SHOW_LOCATE_RESULT,
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_DIST_SQR))
    private static double toroidal$distSqrThroughSeam(BlockPos sourcePos, Vec3i foundPos, Operation<Double> original,
            @Local(argsOnly = true) CommandSourceStack source) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(source.getLevel());
        if (transformer == null) {
            return original.call(sourcePos, foundPos);
        }

        return transformer.sqrDistance(
                sourcePos.getX(), sourcePos.getY(), sourcePos.getZ(), foundPos.getX(), foundPos.getY(), foundPos.getZ());
    }

    @WrapOperation(
            method = SHOW_LOCATE_RESULT,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/commands/LocateCommand;dist(IIII)F"))
    private static float toroidal$flatDistThroughSeam(int fromX, int fromZ, int toX, int toZ,
            Operation<Float> original, @Local(argsOnly = true) CommandSourceStack source) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(source.getLevel());
        if (transformer == null) {
            return original.call(fromX, fromZ, toX, toZ);
        }

        return (float) Math.sqrt(transformer.sqrDistance(fromX, 0.0, fromZ, toX, 0.0, toZ));
    }
}
