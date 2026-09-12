package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackGraphVisualizer;
import com.toroidalworld.InjectionTargets;
import com.toroidalworld.compat.create.CreateInjectionTargets;
import com.toroidalworld.compat.create.client.CreateClientFrame;

import net.createmod.catnip.outliner.Outline;
import net.createmod.catnip.outliner.Outliner;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Mixin(value = TrackGraphVisualizer.class, remap = false)
public class TrackGraphVisualizerMixin {
    @Unique
    private static final String SHOW_LINE =
            "Lnet/createmod/catnip/outliner/Outliner;showLine"
                    + "(Ljava/lang/Object;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)"
                    + "Lnet/createmod/catnip/outliner/Outline$OutlineParams;";

    @WrapOperation(method = {"visualiseSignalEdgeGroups", "debugViewGraph"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/AABB;intersects(Lnet/minecraft/world/phys/AABB;)Z"))
    private static boolean toroidal$foldGraphBounds(AABB graphBounds, AABB viewerBox, Operation<Boolean> original) {
        return original.call(CreateClientFrame.foldBoxToward(viewerBox.getCenter(), graphBounds), viewerBox);
    }

    @WrapOperation(method = {"visualiseSignalEdgeGroups", "debugViewGraph"},
            at = @At(value = "INVOKE",
                    target = InjectionTargets.VEC3_DISTANCE_TO))
    private static double toroidal$foldNodeAgainstCamera(Vec3 node, Vec3 camera, Operation<Double> original) {
        return original.call(CreateClientFrame.nearestCopy(camera, node), camera);
    }

    @WrapOperation(method = {"visualiseSignalEdgeGroups", "debugViewGraph"},
            at = @At(value = "INVOKE",
                    target = CreateInjectionTargets.TRACK_EDGE_GET_POSITION))
    private static Vec3 toroidal$drawInClientFrame(TrackEdge edge, TrackGraph graph, double t,
            Operation<Vec3> original) {
        Vec3 anchor = edge.node1.getLocation().getLocation();
        return CreateClientFrame.inFrameOf(anchor, original.call(edge, graph, t));
    }

    @WrapOperation(method = "debugViewGraph",
            at = {
                @At(value = "INVOKE", target = SHOW_LINE, ordinal = 0),
                @At(value = "INVOKE", target = SHOW_LINE, ordinal = 1)
            })
    private static Outline.OutlineParams toroidal$seatNodeMarker(Outliner outliner, Object slot, Vec3 start,
            Vec3 end, Operation<Outline.OutlineParams> original) {
        return original.call(outliner, slot, CreateClientFrame.inFrameOf(start, start),
                CreateClientFrame.inFrameOf(start, end));
    }
}
