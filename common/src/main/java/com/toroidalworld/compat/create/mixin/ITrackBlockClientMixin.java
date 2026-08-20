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
