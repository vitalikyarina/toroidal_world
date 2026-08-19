package com.toroidalworld.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.entity.SeamRange;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.phys.Vec3;

@Mixin(VibrationSystem.Ticker.class)
public interface VibrationSystemTickerMixin {
    @WrapOperation(
            method = "receiveVibration",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/gameevent/vibrations/VibrationSystem$Listener;distanceBetweenInBlocks(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)F"))
    private static float toroidal$receiveDistThroughSeam(BlockPos origin, BlockPos dest, Operation<Float> original,
            @Local(argsOnly = true) ServerLevel level) {
        return (float) Math.sqrt(SeamRange.sqr(level, origin, dest));
    }

    @WrapOperation(
            method = "tryReloadVibrationParticle",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/gameevent/PositionSource;getPosition(Lnet/minecraft/world/level/Level;)Ljava/util/Optional;"))
    private static Optional<Vec3> toroidal$reloadDestThroughSeam(PositionSource positionSource, Level level,
            Operation<Optional<Vec3>> original, @Local(ordinal = 0) Vec3 origin) {
        Optional<Vec3> destination = original.call(positionSource, level);
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        if (transformer == null || destination.isEmpty()) {
            return destination;
        }

        Vec3 folded = transformer.vectors.nearestCopy(origin, destination.get());
        return folded == destination.get() ? destination : Optional.of(folded);
    }
}
