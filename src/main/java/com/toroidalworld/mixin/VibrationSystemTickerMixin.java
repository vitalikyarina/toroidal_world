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

// The final receiving distance sets the redstone strength a sensor puts out. Across the seam it is a whole world, so a
// sculk sensor would report the weakest possible signal for a vibration a step away. Measured through the seam it is
// right. Travel time was already corrected at scheduling; this is only the strength at the end.
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

    // A vibration whose sensor was loaded from disk mid-flight has its particle re-sent, and where along the way it
    // should restart is found by interpolating from the vibration's origin to the sensor. Across the seam that line
    // runs the long way round the world, so the restart lands somewhere in the middle of it instead of between the two
    // points a few blocks apart. The destination is folded to the copy nearest the origin before the interpolation
    // reads it, which is the only thing on that path measured in raw coordinates.
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
