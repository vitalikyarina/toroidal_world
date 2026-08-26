package com.toroidalworld.mixin;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

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

    // 1.21.1 keeps the raid's level in a field — the methods folded below take no arguments at all, so the level has to
    // be shadowed rather than sugared out of the call.
    @Shadow
    @Final
    private ServerLevel level;

    @ModifyExpressionValue(
            method = "playSound",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;atCenterOf(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$raidHornThroughSeam(Vec3 raidLoc, @Local ServerPlayer listener) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(this.level);
        if (transformer == null) {
            return raidLoc;
        }

        return transformer.nearestCopy(listener.position(), raidLoc);
    }

    @WrapOperation(
            method = "updateRaiders",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;distSqr(Lnet/minecraft/core/Vec3i;)D"))
    private double toroidal$raiderDistanceThroughSeam(BlockPos raidCenter, Vec3i raiderPos,
            Operation<Double> original) {
        return SeamRange.sqr(this.level, raidCenter, raiderPos);
    }

    @WrapOperation(
            method = "moveRaidCenterToNearbyVillageSection",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/SectionPos;cube(Lnet/minecraft/core/SectionPos;I)Ljava/util/stream/Stream;"))
    private Stream<SectionPos> toroidal$villageSectionsThroughSeam(SectionPos cubeCenter, int radius,
            Operation<Stream<SectionPos>> original) {
        Stream<SectionPos> sections = original.call(cubeCenter, radius);
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(this.level);
        if (transformer == null) {
            return sections;
        }

        return sections.map(section -> transformer.fold(section)).distinct();
    }

    @ModifyArg(
            method = "moveRaidCenterToNearbyVillageSection",
            at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;min(Ljava/util/Comparator;)Ljava/util/Optional;"),
            index = 0)
    private Comparator<BlockPos> toroidal$nearestVillageSectionThroughSeam(Comparator<BlockPos> original) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(this.level);
        if (transformer == null) {
            return original;
        }

        ServerLevel raidLevel = this.level;
        BlockPos raidCenter = this.center;
        return Comparator.comparingDouble(pos -> SeamRange.sqr(raidLevel, raidCenter, pos));
    }
}
