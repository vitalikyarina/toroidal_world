package com.toroidalworld.compat.create.mixin;

import java.util.Collection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.simibubi.create.content.trains.graph.TrackNodeLocation.DiscoveredLocation;
import com.simibubi.create.content.trains.track.ITrackBlock;
import com.toroidalworld.compat.create.TrackNodeKeyFold;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

// The walk's own end of the fold, and the one place on the client of a dedicated server where a node key can still be
// built out of bounds. Create reaches the rail across the seam through getBlockState, which this mod already folds, and
// then names the ends of that rail from the raw block position it walked to — a block past the bounds, so an end past
// them too. Every such end is a node the graph would file under a coordinate the world does not have.
//
// A node built here normally folds a step earlier, in its own constructor's in(dimension); this covers the case where
// that step could not resolve a world, which is a client holding only the bounds the server sent it. Taking it at the
// return rather than at the construction is safe for the equals gate inside addToListIfConnected, which decides whether
// the walk continues at all: the end that gate compares against is the one that coincides with the node walked from,
// and that end is produced by stepping back to that node's own coordinate, so it is already canonical whichever side
// of the bounds the block itself lies on. What crosses is the far end, which is exactly what is folded here.
@Mixin(value = ITrackBlock.class, remap = false)
public interface ITrackBlockMixin {
    @Inject(method = "getConnected", at = @At("RETURN"))
    private void toroidal$foldDiscoveredEnds(BlockGetter worldIn, BlockPos pos, BlockState state, boolean linear,
            TrackNodeLocation connectedTo, CallbackInfoReturnable<Collection<DiscoveredLocation>> cir) {
        Level level = worldIn instanceof Level actualLevel ? actualLevel : null;
        for (DiscoveredLocation end : cir.getReturnValue()) {
            ((TrackNodeKeyFold) end).toroidal$foldNodeKey(level);
        }
    }
}
