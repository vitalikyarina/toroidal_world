package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.trains.track.TrackBlock;
import com.toroidalworld.compat.create.CreateSeamFold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

@Mixin(value = TrackBlock.class, remap = false)
public class TrackBlockMixin {
    @WrapOperation(method = "useItemOn",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/structure/BoundingBox;isInside(Lnet/minecraft/core/Vec3i;)Z"))
    private boolean toroidal$clickInAssemblyFrame(BoundingBox area, Vec3i clicked, Operation<Boolean> original,
            @Local(argsOnly = true) Level level) {
        if (!(clicked instanceof BlockPos position)) {
            return original.call(area, clicked);
        }

        return original.call(area, CreateSeamFold.foldPositionToBox(level, area, position));
    }
}
