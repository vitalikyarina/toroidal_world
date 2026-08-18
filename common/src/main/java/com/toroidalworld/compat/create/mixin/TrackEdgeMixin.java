package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
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
