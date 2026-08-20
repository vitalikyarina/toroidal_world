package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import com.toroidalworld.compat.create.BezierCurveFold;

import net.minecraft.world.level.Level;

@Mixin(value = TrackBlockEntity.class, remap = false)
public class TrackBlockEntityMixin {
    @Inject(method = "addConnection", at = @At("HEAD"))
    private void toroidal$foldAddedCurve(BezierConnection connection, CallbackInfo ci) {
        Level level = ((TrackBlockEntity) (Object) this).getLevel();
        ((BezierCurveFold) connection).toroidal$foldCurve(level, level == null ? null : level.dimension());
    }
}
