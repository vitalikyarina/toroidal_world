package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.simibubi.create.content.trains.entity.Navigation;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.toroidalworld.compat.create.CreateInvokeTargets;
import com.toroidalworld.compat.create.CreateSeamFold;
import com.toroidalworld.compat.create.FoldedPoint;

import net.minecraft.world.phys.Vec3;

@Mixin(value = Navigation.class, remap = false)
public abstract class NavigationMixin {
    private static final String SEARCH = "search(DDZLjava/util/ArrayList;"
            + "Lcom/simibubi/create/content/trains/entity/Navigation$StationTest;)V";

    private static final String DESTINATION_FOLD = "toroidal$destinationFold";

    @WrapOperation(method = SEARCH,
            at = @At(value = "INVOKE", target = CreateInvokeTargets.TRACK_NODE_LOCATION_GET_LOCATION, ordinal = 1))
    private Vec3 toroidal$foldRemainingMin(TrackNodeLocation destinationNode, Operation<Vec3> original,
            @Local Vec3 newNodePosition, @Share(DESTINATION_FOLD) LocalRef<FoldedPoint> memo) {
        return toroidal$nearestTo(newNodePosition, destinationNode, original.call(destinationNode), memo);
    }

    @WrapOperation(method = SEARCH,
            at = @At(value = "INVOKE", target = CreateInvokeTargets.TRACK_NODE_LOCATION_GET_LOCATION, ordinal = 2))
    private Vec3 toroidal$foldRemainingMid(TrackNodeLocation destinationNode, Operation<Vec3> original,
            @Local Vec3 newNodePosition, @Share(DESTINATION_FOLD) LocalRef<FoldedPoint> memo) {
        return toroidal$nearestTo(newNodePosition, destinationNode, original.call(destinationNode), memo);
    }

    @WrapOperation(method = SEARCH,
            at = @At(value = "INVOKE", target = CreateInvokeTargets.TRACK_NODE_LOCATION_GET_LOCATION, ordinal = 3))
    private Vec3 toroidal$foldRemainingMax(TrackNodeLocation destinationNode, Operation<Vec3> original,
            @Local Vec3 newNodePosition, @Share(DESTINATION_FOLD) LocalRef<FoldedPoint> memo) {
        return toroidal$nearestTo(newNodePosition, destinationNode, original.call(destinationNode), memo);
    }

    private Vec3 toroidal$nearestTo(Vec3 anchor, TrackNodeLocation destinationNode, Vec3 rawDestination,
            LocalRef<FoldedPoint> memo) {
        FoldedPoint known = memo.get();
        if (known != null && known.isFor(anchor, destinationNode)) {
            return known.value();
        }

        Vec3 folded = CreateSeamFold.nearestCopy(destinationNode.getDimension(), anchor, rawDestination);
        memo.set(new FoldedPoint(anchor, destinationNode, folded));
        return folded;
    }
}
