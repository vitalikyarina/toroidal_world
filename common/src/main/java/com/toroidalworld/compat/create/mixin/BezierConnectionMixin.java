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
import com.toroidalworld.core.WorldFold;

import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

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

        WorldFold transformer = CreateTrackFold.transformerOf(level, this.toroidal$dimension);
        if (transformer == null) {
            return;
        }

        toroidal$canonicaliseEnd(transformer, true);
        toroidal$canonicaliseEnd(transformer, false);

        Vec3 ownerCentre = Vec3.atCenterOf(this.bePositions.getFirst());
        Vec3 rawNearEnd = this.starts.getFirst();
        Vec3 nearEnd = transformer.nearestCopy(ownerCentre, rawNearEnd);
        if (nearEnd != rawNearEnd) {
            this.starts.setFirst(nearEnd);
        }

        Vec3 rawFarEnd = this.starts.getSecond();
        Vec3 farEnd = transformer.nearestCopy(nearEnd, rawFarEnd);
        if (farEnd != rawFarEnd) {
            this.starts.setSecond(farEnd);
        }
    }

    @Unique
    private void toroidal$canonicaliseEnd(WorldFold transformer, boolean first) {
        BlockPos rawEnd = this.bePositions.get(first);
        BlockPos end = transformer.fold(rawEnd);
        if (end != rawEnd) {
            this.bePositions.set(first, end);
        }
    }

    @Inject(method = "secondary", at = @At("RETURN"))
    private void toroidal$foldSwappedCopy(CallbackInfoReturnable<BezierConnection> cir) {
        ((BezierCurveFold) cir.getReturnValue()).toroidal$foldCurve(null, this.toroidal$dimension);
    }

    @Inject(method = "clone()Lcom/simibubi/create/content/trains/track/BezierConnection;", at = @At("RETURN"))
    private void toroidal$carryDimensionToCopy(CallbackInfoReturnable<BezierConnection> cir) {
        ((BezierCurveFold) cir.getReturnValue()).toroidal$foldCurve(null, this.toroidal$dimension);
    }
}
