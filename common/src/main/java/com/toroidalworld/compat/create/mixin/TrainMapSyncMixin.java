package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.compat.trainmap.TrainMapSync;
import com.toroidalworld.compat.create.CreateSeamFold;
import com.toroidalworld.compat.create.TrainMapSyncFold;

@Mixin(value = TrainMapSync.class, remap = false)
public abstract class TrainMapSyncMixin {
    @Inject(method = "createEntry", at = @At("RETURN"))
    private static void toroidal$writeOneFrame(
            CallbackInfoReturnable<TrainMapSync.TrainMapSyncEntry> cir) {
        TrainMapSync.TrainMapSyncEntry entry = cir.getReturnValue();
        if (entry == null) {
            return;
        }

        TrainMapSyncFold.coherent(entry.positions, entry.dimensions,
                dimension -> CreateSeamFold.transformerOf(null, dimension));
    }
}
