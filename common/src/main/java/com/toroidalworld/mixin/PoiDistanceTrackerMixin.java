package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.toroidalworld.accessors.LevelBindable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;

@Mixin(targets = "net.minecraft.world.entity.ai.village.poi.PoiManager$DistanceTracker")
public class PoiDistanceTrackerMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void toroidal$bindOwningLevel(PoiManager owner, CallbackInfo ci) {
        if (((SectionStorageAccessor) owner).toroidal$getLevelHeightAccessor() instanceof ServerLevel level) {
            ((LevelBindable) (Object) this).toroidal$bindLevel(level);
        }
    }
}
