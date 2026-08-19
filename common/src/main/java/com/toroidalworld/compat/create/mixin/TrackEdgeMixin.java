package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackMaterial;
import com.toroidalworld.compat.create.BezierCurveFold;
import com.toroidalworld.compat.create.CreateTrackFold;

import net.minecraft.world.phys.Vec3;

// The other half of one node key, one rail end: with the key canonical, the two ends of a rail that crosses the seam
// are named from opposite edges of the world, and every measurement taken between them reads the long way round. The
// edge is where those two names meet — its length is the distance between them and its position at t is the point
// between them — so it is where they are put back into one frame.
//
// The frame is node1's, and it is kept rather than wrapped: a position along the edge may then sit just past the
// bounds, which is what lets the arithmetic downstream — the spacing between two bogeys, the direction taken as the
// difference of two positions on this same edge — stay ordinary. Everything the edge answers goes through these two
// methods: getDirection and getDirectionAt subtract two of its own positions, and the travelling points advance by its
// length.
//
// The second node is the one folded because the first is the anchor; the wrapped call is therefore the second of the
// two the method makes, and both methods make exactly two. An edge that does not straddle the seam gets its own vector
// back, unchanged and unallocated.
@Mixin(value = TrackEdge.class, remap = false)
public abstract class TrackEdgeMixin {
    @Shadow
    public TrackNode node1;

    // A curved edge answers none of the above: getLength and getPosition hand the question to the curve as soon as one
    // is present, so the folds below never run for it and the frame has to be right in the curve itself. The curve is
    // built from the block entity that stores it, and the constructor is where it meets the node that will be asked
    // about it — the same node, since the edge running the other way carries the swapped copy, whose own first end is
    // this edge's first node. So this is where a curve is told which world it is in, whichever way it arrived: laid by
    // a player, read off disk, or received from the server on a graph the client is only watching.
    @Inject(method = "<init>", at = @At("RETURN"))
    private void toroidal$foldEdgeCurve(TrackNode node1, TrackNode node2, BezierConnection turn,
            TrackMaterial trackMaterial, CallbackInfo ci) {
        if (turn != null) {
            ((BezierCurveFold) turn).toroidal$foldCurve(null, node1.getLocation().getDimension());
        }
    }

    @WrapOperation(method = "getLength",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/trains/graph/TrackNodeLocation;getLocation()Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 1))
    private Vec3 toroidal$foldLengthTarget(TrackNodeLocation target, Operation<Vec3> original) {
        return toroidal$nearestToFirstNode(target, original.call(target));
    }

    @WrapOperation(method = "getPosition",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/trains/graph/TrackNodeLocation;getLocation()Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 1))
    private Vec3 toroidal$foldPositionTarget(TrackNodeLocation target, Operation<Vec3> original) {
        return toroidal$nearestToFirstNode(target, original.call(target));
    }

    // Two edges are checked for a crossing by intersecting the segment between this edge's two nodes with the segment
    // between the other's. Four coordinates, and with canonical keys they can name up to three different copies of the
    // world, so the test either misses a real crossing or invents one. The first node is the frame: the second end of
    // this edge and both ends of the other are brought to the copy nearest it, the other edge's far end against its own
    // near end so that edge keeps its own length while moving as one.
    @WrapOperation(method = "getIntersection",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/trains/graph/TrackNodeLocation;getLocation()Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 1))
    private Vec3 toroidal$foldIntersectionSecondEnd(TrackNodeLocation target, Operation<Vec3> original, TrackNode node1,
            TrackNode node2, TrackEdge other, TrackNode other1, TrackNode other2) {
        return toroidal$nearestTo(node1, target, original.call(target));
    }

    @WrapOperation(method = "getIntersection",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/trains/graph/TrackNodeLocation;getLocation()Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 2))
    private Vec3 toroidal$foldIntersectionOtherNearEnd(TrackNodeLocation target, Operation<Vec3> original,
            TrackNode node1, TrackNode node2, TrackEdge other, TrackNode other1, TrackNode other2) {
        return toroidal$nearestTo(node1, target, original.call(target));
    }

    @WrapOperation(method = "getIntersection",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/trains/graph/TrackNodeLocation;getLocation()Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 3))
    private Vec3 toroidal$foldIntersectionOtherFarEnd(TrackNodeLocation target, Operation<Vec3> original,
            TrackNode node1, TrackNode node2, TrackEdge other, TrackNode other1, TrackNode other2) {
        Vec3 otherNearEnd = toroidal$nearestTo(node1, other1.getLocation(), other1.getLocation().getLocation());
        return toroidal$folded(target, otherNearEnd, original.call(target));
    }

    private Vec3 toroidal$nearestTo(TrackNode anchorNode, TrackNodeLocation target, Vec3 rawTarget) {
        return toroidal$folded(target, anchorNode.getLocation().getLocation(), rawTarget);
    }

    private Vec3 toroidal$nearestToFirstNode(TrackNodeLocation target, Vec3 rawTarget) {
        return toroidal$folded(target, this.node1.getLocation().getLocation(), rawTarget);
    }

    private Vec3 toroidal$folded(TrackNodeLocation target, Vec3 anchor, Vec3 rawTarget) {
        return CreateTrackFold.nearestCopy(target.getDimension(), anchor, rawTarget);
    }
}
