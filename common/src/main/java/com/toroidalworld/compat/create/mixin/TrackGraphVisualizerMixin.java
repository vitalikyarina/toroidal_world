package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackGraphVisualizer;
import com.toroidalworld.compat.create.client.CreateClientFrame;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// The coloured line Create lays along the rails to show which signal block each stretch belongs to, drawn whenever a
// Train Signal is in hand, and the same overlay again in the F3 graph view. Both are the same crossing as the hover
// preview: the graph is canonical, the client's world is continuous around the player, and this code compares one
// against the other and then draws into the second from the first.
//
// Three places, one statement. The graph's own bounds decide whether the viewer is near it at all; the per-node cull
// asks the same of each node, and the dedup test beside it asks it of the node at the far end; and every point of
// every line comes out of the edge in canonical coordinates. Left alone, a graph across the seam is drawn only for
// the half whose canonical names happen to lie near the client, so the line stops at the boundary — and a graph whose
// whole span lies on the far side fails the bounds test and is not drawn at all.
//
// Each fold's reference is the thing the value is about to be measured or drawn against, which here is the camera —
// the two distance sites carry it as their own argument, and the drawing site takes it from the client, having none.
@Mixin(value = TrackGraphVisualizer.class, remap = false)
public class TrackGraphVisualizerMixin {
    @WrapOperation(method = {"visualiseSignalEdgeGroups", "debugViewGraph"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/AABB;intersects(Lnet/minecraft/world/phys/AABB;)Z"))
    private static boolean toroidal$foldGraphBounds(AABB graphBounds, AABB viewerBox, Operation<Boolean> original) {
        return original.call(CreateClientFrame.foldBoxToward(viewerBox.getCenter(), graphBounds), viewerBox);
    }

    @WrapOperation(method = {"visualiseSignalEdgeGroups", "debugViewGraph"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;distanceTo(Lnet/minecraft/world/phys/Vec3;)D"))
    private static double toroidal$foldNodeAgainstCamera(Vec3 node, Vec3 camera, Operation<Double> original) {
        return original.call(CreateClientFrame.nearestCopy(camera, node), camera);
    }

    @WrapOperation(method = {"visualiseSignalEdgeGroups", "debugViewGraph"},
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/trains/graph/TrackEdge;getPosition(Lcom/simibubi/create/content/trains/graph/TrackGraph;D)Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 toroidal$drawInClientFrame(TrackEdge edge, TrackGraph graph, double t,
            Operation<Vec3> original) {
        return CreateClientFrame.nearestCopy(CreateClientFrame.camera(), original.call(edge, graph, t));
    }
}
