package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.contraptions.actors.roller.PaveTask;
import com.simibubi.create.content.contraptions.actors.roller.TrackPaverV2;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.toroidalworld.compat.create.CreateTrackFold;

import net.minecraft.world.phys.Vec3;

@Mixin(value = TrackPaverV2.class, remap = false)
public abstract class TrackPaverV2Mixin {
    @WrapOperation(method = "pave",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/trains/graph/TrackNodeLocation;getLocation()Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 1))
    private static Vec3 toroidal$foldSecondNode(TrackNodeLocation target, Operation<Vec3> original, PaveTask task,
            TrackGraph graph, TrackEdge edge, double from, double to) {
        return CreateTrackFold.nearestCopy(target.getDimension(), edge.node1.getLocation().getLocation(),
                original.call(target));
    }
}
