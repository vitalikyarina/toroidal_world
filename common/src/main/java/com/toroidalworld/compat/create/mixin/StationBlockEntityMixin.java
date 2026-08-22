package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import com.toroidalworld.compat.create.CreateSeamFold;
import com.toroidalworld.compat.create.CreateTrackFold;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;

@Mixin(value = StationBlockEntity.class, remap = false)
public class StationBlockEntityMixin {
    @ModifyVariable(method = "trackClicked", argsOnly = true,
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/trains/station/StationBlockEntity;refreshAssemblyInfo()V",
                    shift = At.Shift.AFTER))
    private BlockPos toroidal$clickedInAssemblyFrame(BlockPos clicked) {
        StationBlockEntity station = (StationBlockEntity) (Object) this;
        Level level = station.getLevel();
        BoundingBox area = level == null
                ? null
                : StationBlockEntity.assemblyAreas.get(level).get(station.getBlockPos());
        return area == null ? clicked : CreateSeamFold.foldPositionToBox(level, area, clicked);
    }

    @WrapOperation(method = "assemble",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;subtract(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$trackEndInTheAssemblingFrame(Vec3 end, Vec3 center, Operation<Vec3> original) {
        Level level = ((StationBlockEntity) (Object) this).getLevel();
        return original.call(end, CreateTrackFold.nearestCopy(level, end, center));
    }
}
