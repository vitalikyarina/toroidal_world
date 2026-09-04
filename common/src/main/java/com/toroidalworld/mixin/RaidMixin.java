package com.toroidalworld.mixin;

import java.util.Comparator;
import java.util.stream.Stream;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.toroidalworld.InjectionTargets;
import com.toroidalworld.core.FoldedOrder;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.entity.SeamRange;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.phys.Vec3;

@Mixin(Raid.class)
public class RaidMixin {
    @Shadow
    private BlockPos center;

    @ModifyExpressionValue(
            method = "playSound",
            at = @At(
                    value = "INVOKE",
                    target = InjectionTargets.VEC3_AT_CENTER_OF))
    private Vec3 toroidal$raidHornThroughSeam(Vec3 raidLoc, @Local(argsOnly = true) ServerLevel level,
            @Local ServerPlayer listener) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        if (transformer == null) {
            return raidLoc;
        }

        return transformer.nearestCopy(listener.position(), raidLoc);
    }

    @WrapOperation(
            method = "updateRaiders",
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_DIST_SQR))
    private double toroidal$raiderDistanceThroughSeam(BlockPos raidCenter, Vec3i raiderPos, Operation<Double> original,
            @Local(argsOnly = true) ServerLevel level) {
        return SeamRange.sqr(level, raidCenter, raiderPos);
    }

    @WrapOperation(
            method = "moveRaidCenterToNearbyVillageSection",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/SectionPos;cube(Lnet/minecraft/core/SectionPos;I)Ljava/util/stream/Stream;"))
    private Stream<SectionPos> toroidal$villageSectionsThroughSeam(SectionPos cubeCenter, int radius,
            Operation<Stream<SectionPos>> original, @Local(argsOnly = true) ServerLevel level) {
        Stream<SectionPos> sections = original.call(cubeCenter, radius);
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        if (transformer == null) {
            return sections;
        }

        return sections.map(section -> transformer.fold(section)).distinct();
    }

    @ModifyArg(
            method = "moveRaidCenterToNearbyVillageSection",
            at = @At(value = "INVOKE", target = InjectionTargets.STREAM_MIN),
            index = 0)
    private Comparator<BlockPos> toroidal$nearestVillageSectionThroughSeam(Comparator<BlockPos> original,
            @Local(argsOnly = true) ServerLevel level) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        if (transformer == null) {
            return original;
        }

        return FoldedOrder.around(original, transformer, this.center);
    }
}
