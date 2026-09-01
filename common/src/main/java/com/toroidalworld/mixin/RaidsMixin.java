package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.accessors.LevelBindable;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.entity.SeamRange;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.raid.Raids;

@Mixin(Raids.class)
public class RaidsMixin implements LevelBindable {
    @Unique
    private @Nullable ServerLevel toroidal$level;

    @Override
    public void toroidal$bindLevel(ServerLevel level) {
        this.toroidal$level = level;
    }

    @WrapOperation(
            method = "getNearbyRaid",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;distSqr(Lnet/minecraft/core/Vec3i;)D"))
    private double toroidal$raidDistanceThroughSeam(BlockPos raidCenter, Vec3i pos, Operation<Double> original) {
        ServerLevel level = this.toroidal$level;
        return level == null ? original.call(raidCenter, pos) : SeamRange.sqr(level, raidCenter, pos);
    }

    @WrapOperation(
            method = "createOrExtendRaid",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/village/poi/PoiRecord;getPos()Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$poiInPlayerFrame(PoiRecord record, Operation<BlockPos> original,
            @Local(argsOnly = true) ServerPlayer player, @Local(argsOnly = true) BlockPos raidPosition) {
        BlockPos pos = original.call(record);
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(player.level());
        if (transformer == null) {
            return pos;
        }

        return transformer.nearestCopy(raidPosition, pos);
    }

    @WrapOperation(
            method = "createOrExtendRaid",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;containing(Lnet/minecraft/core/Position;)Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$centerIntoBounds(Position mean, Operation<BlockPos> original,
            @Local(argsOnly = true) ServerPlayer player) {
        BlockPos center = original.call(mean);
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(player.level());
        if (transformer == null) {
            return center;
        }

        return transformer.fold(center);
    }
}
