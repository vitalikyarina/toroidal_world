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
