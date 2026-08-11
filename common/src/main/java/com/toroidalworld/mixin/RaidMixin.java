package com.toroidalworld.mixin;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.toroidalworld.core.WorldLoopTransformer;
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

// The raid's own housekeeping measures raw distances too. A raider that chases a villager across the seam reads a
// world away from the centre and is silently dropped from the raid — the wave "dies" while its raiders fight on; and
// a centre that lost its village status searches for replacement village ground in raw section coordinates, blind to
// the half of the village lying across the seam, and declares the raid lost with the village still standing.
@Mixin(Raid.class)
public class RaidMixin {
    @Shadow
    private BlockPos center;

    // 1.21.1 keeps the raid's level in a field — the methods folded below take no arguments at all, so the level has to
    // be shadowed rather than sugared out of the call.
    @Shadow
    @Final
    private ServerLevel level;

    // The horn is the one raid sound the raid aims itself instead of handing to PlayerList.broadcast, so the seam-aware
    // distance that covers every other sound never touches it. Both of its readings are taken in raw coordinates: the
    // 64-block gate that decides who hears it at all, and the 13 blocks the horn is pinned to from the listener along the
    // direction to the raid. Across the seam that direction points the long way round and that distance is a whole world,
    // so a player twenty blocks from the raid is told nothing — unless the raid bossbar already holds them, and then the
    // horn blows from behind them.
    //
    // Both readings are built from this one point, so folding the raid to the copy nearest the listener is the whole fix:
    // the gate then measures the walk that exists, and the horn is pinned toward the raid where it really stands.
    // Vanilla's 13 and 64 blocks are untouched, and a horn placed beyond the bounds is what the packet translation is
    // for — it lays the position back into the listener's own space.
    @ModifyExpressionValue(
            method = "playSound",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;atCenterOf(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$raidHornThroughSeam(Vec3 raidLoc, @Local ServerPlayer listener) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(this.level);
        Vec3 folded = transformer == null
                ? raidLoc
                : transformer.vectors.nearestCopy(listener.position(), raidLoc);

        return folded;
    }

    @WrapOperation(
            method = "updateRaiders",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;distSqr(Lnet/minecraft/core/Vec3i;)D"))
    private double toroidal$raiderDistanceThroughSeam(BlockPos raidCenter, Vec3i raiderPos,
            Operation<Double> original) {
        return SeamRange.sqr(this.level, raidCenter, raiderPos);
    }

    // The candidate cube is walked in raw section coordinates, and the village-distance tracker only ever holds
    // canonical sections — a section past the bounds is never a village, whatever stands there. Each candidate is
    // restated as the section that physically exists; Y has no seam and passes through.
    @WrapOperation(
            method = "moveRaidCenterToNearbyVillageSection",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/SectionPos;cube(Lnet/minecraft/core/SectionPos;I)Ljava/util/stream/Stream;"))
    private Stream<SectionPos> toroidal$villageSectionsThroughSeam(SectionPos cubeCenter, int radius,
            Operation<Stream<SectionPos>> original) {
        List<SectionPos> raw = original.call(cubeCenter, radius).toList();
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(this.level);
        List<SectionPos> folded = transformer == null
                ? raw
                : raw.stream().map(transformer.chunks::wrapSection).distinct().toList();

        return folded.stream();
    }

    // With the candidates canonical, the winner must be picked by the distance the world actually walks: a section
    // across the seam reads raw-far and would lose to any same-side candidate, however close it really is.
    @ModifyArg(
            method = "moveRaidCenterToNearbyVillageSection",
            at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;min(Ljava/util/Comparator;)Ljava/util/Optional;"),
            index = 0)
    private Comparator<BlockPos> toroidal$nearestVillageSectionThroughSeam(Comparator<BlockPos> original) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(this.level);
        if (transformer == null) {
            return original;
        }

        ServerLevel raidLevel = this.level;
        BlockPos raidCenter = this.center;
        return Comparator.comparingDouble(pos -> SeamRange.sqr(raidLevel, raidCenter, pos));
    }
}
