package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.trains.graph.TrackGraphHelper;
import com.toroidalworld.compat.create.CreateTrackFold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// Where a signal or a station asks which piece of track it is attached to. The answer is decided by direction: the
// offset from the block's own centre to each end of its rail says whether that end lies forward or backward along the
// targeted axis, and the walk that follows compares each step's own offset the same way. Near the seam one of those
// two coordinates is canonical and the other names the far side of the world, so the offset points the opposite way,
// neither test passes and the block reports no track at all — a signal one block from the bounds with a rail running
// through it.
//
// Both subtractions are the same statement — the position being measured, brought to the copy of itself nearest the
// one it is measured from — so one handler serves the ends of the rail and every step of the walk, and the distance
// the walk accumulates comes out in blocks travelled rather than worlds. The level is the method's own parameter,
// which is what makes this work on a client with no server to ask.
@Mixin(value = TrackGraphHelper.class, remap = false)
public abstract class TrackGraphHelperMixin {
    @WrapOperation(method = "getGraphLocationAt",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;subtract(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 toroidal$foldWalkDelta(Vec3 target, Vec3 anchor, Operation<Vec3> original, Level level,
            BlockPos pos, AxisDirection targetDirection, Vec3 targetAxis) {
        return original.call(CreateTrackFold.nearestCopy(level, anchor, target), anchor);
    }

    // The single-piece case, where the position along the edge is half the distance between its two nodes.
    @WrapOperation(method = "getGraphLocationAt",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;distanceTo(Lnet/minecraft/world/phys/Vec3;)D"))
    private static double toroidal$foldNodeDistance(Vec3 anchor, Vec3 target, Operation<Double> original, Level level,
            BlockPos pos, AxisDirection targetDirection, Vec3 targetAxis) {
        return original.call(anchor, CreateTrackFold.nearestCopy(level, anchor, target));
    }
}
