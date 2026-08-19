package com.toroidalworld.compat.create.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.content.trains.track.BezierConnection;
import com.toroidalworld.compat.create.BezierCurveFold;
import com.toroidalworld.compat.create.CreateTrackFold;
import com.toroidalworld.core.WorldLoopTransformer;

import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// One curve, expressed from the block entity that holds it. bePositions name two real blocks and stay canonical —
// they are the keys the connection map and validateConnections are built on — while starts are geometry, and geometry
// has to be continuous: the first end on the owner's own rail, the second end the copy nearest it, which for a curve
// across the seam sits just past the bounds. Everything the curve derives — the length, the segment count, the step
// LUT, the bounds, every position and normal along it — comes from that pair, so putting the pair in one frame settles
// all of them at once, and the delta each copy writes to disk stays a few blocks rather than a world.
//
// secondary() is where the frame is lost rather than where it is used. It swaps which end is first and leaves the
// coordinates alone, so the copy stored on the far block entity, and the reverse edge TrackGraph builds out of the same
// call, would answer from the frame of the end they no longer belong to. Folding here covers that call and the four
// others — validateConnections, writeTurns, transform, the graph's own reverse edge — instead of each of them.
//
// The fold is idempotent and the vector fold hands back its argument when nothing moves, so a curve laid inland is
// this comparison and no allocation, whichever copy of itself it is being asked about.
@Mixin(value = BezierConnection.class, remap = false)
public abstract class BezierConnectionMixin implements BezierCurveFold {
    @Shadow
    @Final
    public Couple<BlockPos> bePositions;

    @Shadow
    @Final
    public Couple<Vec3> starts;

    @Unique
    private @Nullable ResourceKey<Level> toroidal$dimension;

    @Override
    public void toroidal$foldCurve(@Nullable Level level, @Nullable ResourceKey<Level> dimension) {
        if (dimension != null) {
            this.toroidal$dimension = dimension;
        }

        WorldLoopTransformer transformer = CreateTrackFold.transformerOf(level, this.toroidal$dimension);
        if (transformer == null) {
            return;
        }

        toroidal$canonicaliseEnd(transformer, true);
        toroidal$canonicaliseEnd(transformer, false);

        Vec3 ownerCentre = Vec3.atCenterOf(this.bePositions.getFirst());
        Vec3 rawNearEnd = this.starts.getFirst();
        Vec3 nearEnd = transformer.vectors.nearestCopy(ownerCentre, rawNearEnd);
        if (nearEnd != rawNearEnd) {
            this.starts.setFirst(nearEnd);
        }

        Vec3 rawFarEnd = this.starts.getSecond();
        Vec3 farEnd = transformer.vectors.nearestCopy(nearEnd, rawFarEnd);
        if (farEnd != rawFarEnd) {
            this.starts.setSecond(farEnd);
        }
    }

    // The two block entity positions are keys before they are geometry: the connection map files a curve under
    // bePositions.getSecond(), validateConnections drops one whose first entry is not its owner's own position, and a
    // block entity that took its curve straight from placement can hold an end named from past the bounds.
    @Unique
    private void toroidal$canonicaliseEnd(WorldLoopTransformer transformer, boolean first) {
        BlockPos rawEnd = this.bePositions.get(first);
        BlockPos end = transformer.blocks.wrap(rawEnd);
        if (end != rawEnd) {
            this.bePositions.set(first, end);
        }
    }

    @Inject(method = "secondary", at = @At("RETURN"))
    private void toroidal$foldSwappedCopy(CallbackInfoReturnable<BezierConnection> cir) {
        ((BezierCurveFold) cir.getReturnValue()).toroidal$foldCurve(null, this.toroidal$dimension);
    }

    // The clone keeps the frame it was copied from, so this carries the dimension across rather than moving anything —
    // without it a curve cloned before it is written or restored would have forgotten which world it folds by.
    @Inject(method = "clone()Lcom/simibubi/create/content/trains/track/BezierConnection;", at = @At("RETURN"))
    private void toroidal$carryDimensionToCopy(CallbackInfoReturnable<BezierConnection> cir) {
        ((BezierCurveFold) cir.getReturnValue()).toroidal$foldCurve(null, this.toroidal$dimension);
    }
}
