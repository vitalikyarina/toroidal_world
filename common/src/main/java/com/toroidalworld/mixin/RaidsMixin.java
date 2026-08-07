package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.accessors.LevelBindable;
import com.toroidalworld.core.WorldLoopTransformer;
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

// Every "is there a raid here" question in the game — the boss bar, Bad Omen starting or joining, /raid check, the
// villager panic AI, raiders finding their own raid — funnels through getNearbyRaid, and it measures with a raw
// distance: a player a step across the seam reads most of a world away from the raid centre and stops existing for
// the raid, while the raid ticks on without them. Folding this one measurement covers every asker at once.
//
// Raids is saved data and never sees the level it belongs to, so the level is bound at the level's own construction,
// the same way the tick containers are.
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

    // The raid centre is the plain mean of the village POI positions, and the POI search is already seam-aware — so a
    // village straddling the seam contributes both halves in canonical coordinates, and the raw mean lands mid-world,
    // half a world from the village. Every POI is instead read in the player's own frame: its nearest copy relative to
    // where the omen walked in. The 64-block POI radius bounds the whole cluster to a quarter of the narrowest world
    // that can wrap, so the unwrap is unambiguous and the mean is exact — no circular statistics needed.
    @WrapOperation(
            method = "createOrExtendRaid",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/village/poi/PoiRecord;getPos()Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$poiInPlayerFrame(PoiRecord record, Operation<BlockPos> original,
            @Local(argsOnly = true) ServerPlayer player, @Local(argsOnly = true) BlockPos raidPosition) {
        BlockPos pos = original.call(record);
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(player.level());
        if (transformer == null) {
            return pos;
        }

        return transformer.blocks.unwrap(raidPosition, pos);
    }

    // The mean of positions unwrapped around the player may itself lie past the bounds; the centre the raid keeps must
    // be the canonical one, like every other stored position.
    @WrapOperation(
            method = "createOrExtendRaid",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;containing(Lnet/minecraft/core/Position;)Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$centerIntoBounds(Position mean, Operation<BlockPos> original,
            @Local(argsOnly = true) ServerPlayer player) {
        BlockPos center = original.call(mean);
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(player.level());
        if (transformer == null) {
            return center;
        }

        return transformer.blocks.wrap(center);
    }
}
