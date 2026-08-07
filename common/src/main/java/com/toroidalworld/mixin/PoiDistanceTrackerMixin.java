package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.toroidalworld.accessors.LevelBindable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;

// The village distance graph is the one SectionTracker vanilla builds, and like every other tracker it is handed no
// level: it lives as an inner class of the POI manager and reaches its data through the outer instance. SectionTracker
// itself cannot ask for that instance — a mixin on the base class has no way to name a subclass's enclosing object — so
// the binding is done from the one place that holds both, the tracker's own constructor, whose synthetic first argument
// IS the owning manager.
//
// Which level that manager serves is not its own field either; it is the height accessor SectionStorage keeps, and for
// a server manager that accessor is the ServerLevel. A client has no POI manager at all, so the type test is what keeps
// this to the server without asking about sides.
//
// The level, not the transformer: the manager is built while the ChunkMap is still being constructed, and the level's
// transformer comes from a generator that is not finished yet. SectionTrackerMixin resolves it on first use.
@Mixin(targets = "net.minecraft.world.entity.ai.village.poi.PoiManager$DistanceTracker")
public class PoiDistanceTrackerMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void toroidal$bindOwningLevel(PoiManager owner, CallbackInfo ci) {
        if (((SectionStorageAccessor) owner).toroidal$getLevelHeightAccessor() instanceof ServerLevel level) {
            ((LevelBindable) (Object) this).toroidal$bindLevel(level);
        }
    }
}
