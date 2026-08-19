package com.toroidalworld.compat.create.mixin;

import java.util.Collection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.simibubi.create.content.trains.track.ITrackBlock;
import com.toroidalworld.compat.create.client.CreateClientFrame;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

// The one step of the track graph that turns a node key back into ground. allAdjacent is the conversion itself, and
// every client-side traversal of the graph goes through this walk to reach it — the other caller of that conversion,
// TrackPropagator, is the server's own graph maintenance and needs nothing, because there the canonical frame is the
// world's.
//
// The fold cannot live on allAdjacent, which is where it first looks like it belongs: the statement is about the level
// doing the reading, and that method has no level and no way to get one, so folding there would move the server's
// walk too — in singleplayer both sides share the one loaded class. This call site is the only place carrying both
// halves, and it is a choke point rather than a call site picked for convenience.
//
// Taken on the conversion rather than on the block read below it, because the position is used twice: once to ask
// what block is there, and again to ask that block for its own connected ends, which reads the world at it for the
// elevation and the axes. One of the two folded and the other not would read a rail and then measure it as air.
@Mixin(value = ITrackBlock.class, remap = false)
public interface ITrackBlockClientMixin {
    @WrapOperation(method = "walkConnectedTracks",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/trains/graph/TrackNodeLocation;allAdjacent()Ljava/util/Collection;"))
    private static Collection<BlockPos> toroidal$walkInClientFrame(TrackNodeLocation location,
            Operation<Collection<BlockPos>> original, BlockGetter worldIn, TrackNodeLocation walkedFrom,
            boolean linear) {
        return CreateClientFrame.nearestCopies(worldIn, original.call(location));
    }
}
