package com.toroidalworld.mixin;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.InjectionTargets;
import com.toroidalworld.core.FoldedOrder;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.portal.PortalForcer;

@Mixin(PortalForcer.class)
public class PortalForcerMixin {
    @Shadow
    @Final
    protected ServerLevel level;

    @WrapOperation(
            method = "findClosestPortalPosition",
            at = @At(value = "INVOKE", target = InjectionTargets.STREAM_MIN))
    private Optional<BlockPos> toroidal$nearestThroughSeam(
            Stream<BlockPos> candidates,
            Comparator<BlockPos> byDistance,
            Operation<Optional<BlockPos>> original,
            BlockPos approximateExitPos,
            boolean toNether,
            WorldBorder worldBorder) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(this.level);
        if (transformer == null) {
            return original.call(candidates, byDistance);
        }

        return original.call(candidates, FoldedOrder.around(byDistance, transformer, approximateExitPos));
    }
}
