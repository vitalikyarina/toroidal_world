package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import com.simibubi.create.compat.trainmap.TrainMapSync;
import com.toroidalworld.compat.create.TrainMapSyncFold;
import com.toroidalworld.compat.create.client.TrainMapViewFold;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

@Mixin(value = TrainMapSync.TrainMapSyncEntry.class, remap = false)
public abstract class TrainMapSyncEntryMixin {
    @Shadow
    public Float[] positions;

    @Shadow
    public Float[] prevPositions;

    @Shadow
    public List<ResourceKey<Level>> dimensions;

    @Inject(method = "updateFrom", at = @At("RETURN"))
    private void toroidal$carryPreviousAcrossSeam(TrainMapSync.TrainMapSyncEntry other, boolean light,
            CallbackInfo ci) {
        TrainMapSyncFold.rebaseOnto(this.prevPositions, this.positions, this.dimensions,
                dimension -> TrainMapViewFold.transformer());
    }
}
