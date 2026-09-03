package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackMaterial;
import com.toroidalworld.compat.create.CreateInvokeTargets;
import com.toroidalworld.compat.create.BezierCurveFold;
import com.toroidalworld.compat.create.CreateSeamFold;

import net.minecraft.world.phys.Vec3;

@Mixin(value = TrackEdge.class, remap = false)
public abstract class TrackEdgeMixin {
    private static final String FIRST_NODE_ANCHOR = "toroidal$firstNodeAnchor";
    private static final String OTHER_NEAR_END = "toroidal$otherNearEnd";

    @Shadow
    public TrackNode node1;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void toroidal$foldEdgeCurve(TrackNode node1, TrackNode node2, BezierConnection turn,
            TrackMaterial trackMaterial, CallbackInfo ci) {
        if (turn != null) {
            ((BezierCurveFold) turn).toroidal$foldCurve(null, node1.getLocation().getDimension());
        }
    }

    @WrapOperation(method = "getLength",
            at = @At(value = "INVOKE",
                    target = CreateInvokeTargets.TRACK_NODE_LOCATION_GET_LOCATION,
                    ordinal = 1))
    private Vec3 toroidal$foldLengthTarget(TrackNodeLocation target, Operation<Vec3> original) {
        return toroidal$nearestToFirstNode(target, original.call(target));
    }

    @WrapOperation(method = "getPosition",
            at = @At(value = "INVOKE",
                    target = CreateInvokeTargets.TRACK_NODE_LOCATION_GET_LOCATION,
                    ordinal = 1))
    private Vec3 toroidal$foldPositionTarget(TrackNodeLocation target, Operation<Vec3> original) {
        return toroidal$nearestToFirstNode(target, original.call(target));
    }

    @WrapOperation(method = "getPositionSmoothed",
            at = @At(value = "INVOKE",
                    target = "Lnet/createmod/catnip/math/VecHelper;lerp(FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$foldSmoothedEnds(float t, Vec3 fromNode1, Vec3 fromNode2, Operation<Vec3> original) {
        TrackNodeLocation location = this.node1.getLocation();
        Vec3 anchor = location.getLocation();
        Vec3 nearEnd = CreateSeamFold.nearestCopy(location.getDimension(), anchor, fromNode1);
        Vec3 farEnd = CreateSeamFold.nearestCopy(location.getDimension(), nearEnd, fromNode2);
        return original.call(t, nearEnd, farEnd);
    }

    @WrapOperation(method = "getIntersection",
            at = @At(value = "INVOKE",
                    target = CreateInvokeTargets.TRACK_NODE_LOCATION_GET_LOCATION,
                    ordinal = 1))
    private Vec3 toroidal$foldIntersectionSecondEnd(TrackNodeLocation target, Operation<Vec3> original, TrackNode node1,
            TrackNode node2, TrackEdge other, TrackNode other1, TrackNode other2,
            @Share(FIRST_NODE_ANCHOR) LocalRef<Vec3> anchorRef) {
        return toroidal$folded(target, toroidal$anchorOf(node1, anchorRef), original.call(target));
    }

    @WrapOperation(method = "getIntersection",
            at = @At(value = "INVOKE",
                    target = CreateInvokeTargets.TRACK_NODE_LOCATION_GET_LOCATION,
                    ordinal = 2))
    private Vec3 toroidal$foldIntersectionOtherNearEnd(TrackNodeLocation target, Operation<Vec3> original,
            TrackNode node1, TrackNode node2, TrackEdge other, TrackNode other1, TrackNode other2,
            @Share(FIRST_NODE_ANCHOR) LocalRef<Vec3> anchorRef,
            @Share(OTHER_NEAR_END) LocalRef<Vec3> otherNearEndRef) {
        Vec3 folded = toroidal$folded(target, toroidal$anchorOf(node1, anchorRef), original.call(target));
        otherNearEndRef.set(folded);
        return folded;
    }

    @WrapOperation(method = "getIntersection",
            at = @At(value = "INVOKE",
                    target = CreateInvokeTargets.TRACK_NODE_LOCATION_GET_LOCATION,
                    ordinal = 3))
    private Vec3 toroidal$foldIntersectionOtherFarEnd(TrackNodeLocation target, Operation<Vec3> original,
            TrackNode node1, TrackNode node2, TrackEdge other, TrackNode other1, TrackNode other2,
            @Share(FIRST_NODE_ANCHOR) LocalRef<Vec3> anchorRef,
            @Share(OTHER_NEAR_END) LocalRef<Vec3> otherNearEndRef) {
        Vec3 otherNearEnd = otherNearEndRef.get();
        if (otherNearEnd == null) {
            otherNearEnd = toroidal$folded(other1.getLocation(), toroidal$anchorOf(node1, anchorRef),
                    other1.getLocation().getLocation());
        }

        return toroidal$folded(target, otherNearEnd, original.call(target));
    }

    private Vec3 toroidal$anchorOf(TrackNode node, LocalRef<Vec3> anchorRef) {
        Vec3 anchor = anchorRef.get();
        if (anchor == null) {
            anchor = node.getLocation().getLocation();
            anchorRef.set(anchor);
        }

        return anchor;
    }

    private Vec3 toroidal$nearestToFirstNode(TrackNodeLocation target, Vec3 rawTarget) {
        return toroidal$folded(target, this.node1.getLocation().getLocation(), rawTarget);
    }

    private Vec3 toroidal$folded(TrackNodeLocation target, Vec3 anchor, Vec3 rawTarget) {
        return CreateSeamFold.nearestCopy(target.getDimension(), anchor, rawTarget);
    }
}
