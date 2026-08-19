package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.trains.track.TrackPlacement;
import com.toroidalworld.compat.create.CreateTrackFold;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

// Laying track takes two clicks, and the first one is kept in an item component until the second arrives. Both blocks
// are canonical, each canonicalized on its own click, so a pair straddling the seam reads about a world apart — and the
// very first thing tryConnect does with that pair is measure it against maxTrackPlacementLength, which is 32 blocks by
// default and 128 at its ceiling. Nothing is drawn wrong; the placement is refused outright, curve and straight batch
// alike, in every world small enough to be worth looping.
//
// So the fold goes in before the gate, on the stored click rather than the fresh one: the clicked block is what the
// player is looking at and everything else in the method is measured from it. Two values carry the frame — the block
// and the curve end sitting on it — and folding each against the clicked block gives them the same lap, because they
// are a block apart and the alternative copy is half a world away.
//
// Everything downstream then works in one frame with no further help. The stored end is re-derived from the folded
// block a few lines later, so it stays folded; the gate, the intersection, the turn angle and the ascend are ordinary
// arithmetic on a pair a few blocks apart; and the positions that reach the world — the blocks laid along each
// extension, the two block entities the curve is handed to — go through Level, which wraps what it is handed. The
// curve itself is put back into each block entity's own frame when it is stored, which is BezierConnectionMixin.
//
// Both sides run this. The client's preview measures the same gate and draws the same curve, and it reads the bounds
// the server sent it rather than its own, which are deliberately absent.
@Mixin(value = TrackPlacement.class, remap = false)
public class TrackPlacementMixin {
    @ModifyExpressionValue(
            method = "tryConnect",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/trains/track/TrackPlacement$ConnectingFrom;pos()Lnet/minecraft/core/BlockPos;"))
    private static BlockPos toroidal$foldStoredBlock(BlockPos storedPos, Level level, Player player, BlockPos clickedPos,
            BlockState clickedState, ItemStack stack, boolean girder, boolean maximiseTurn) {
        return CreateTrackFold.nearestCopy(level, clickedPos, storedPos);
    }

    @ModifyExpressionValue(
            method = "tryConnect",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/trains/track/TrackPlacement$ConnectingFrom;end()Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 toroidal$foldStoredEnd(Vec3 storedEnd, Level level, Player player, BlockPos clickedPos,
            BlockState clickedState, ItemStack stack, boolean girder, boolean maximiseTurn) {
        return CreateTrackFold.nearestCopy(level, Vec3.atCenterOf(clickedPos), storedEnd);
    }
}
