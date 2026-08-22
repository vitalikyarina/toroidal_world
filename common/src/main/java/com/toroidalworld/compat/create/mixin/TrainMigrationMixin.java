package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.trains.entity.TrainMigration;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.toroidalworld.compat.create.CreateTrackFold;

import net.createmod.catnip.data.Couple;
import net.minecraft.world.phys.Vec3;

@Mixin(value = TrainMigration.class, remap = false)
public abstract class TrainMigrationMixin {
    @Shadow
    Couple<TrackNodeLocation> locations;

    @Shadow
    Vec3 fallback;

    @WrapOperation(method = "tryMigratingTo",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/trains/graph/TrackNodeLocation;getLocation()Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 0))
    private Vec3 toroidal$foldOldEdgeEnd(TrackNodeLocation end, Operation<Vec3> original) {
        Vec3 start = this.locations.getFirst().getLocation();
        return CreateTrackFold.nearestCopy(end.getDimension(), start, original.call(end));
    }

    @WrapOperation(method = "tryMigratingTo",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/trains/graph/TrackNodeLocation;getLocation()Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 2))
    private Vec3 toroidal$foldCandidateNode(TrackNodeLocation candidate, Operation<Vec3> original) {
        return CreateTrackFold.nearestCopy(candidate.getDimension(), this.fallback, original.call(candidate));
    }
}
