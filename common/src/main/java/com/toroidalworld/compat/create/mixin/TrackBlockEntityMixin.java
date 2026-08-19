package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import com.toroidalworld.compat.create.BezierCurveFold;

import net.minecraft.world.level.Level;

// Where a curve first meets a block entity that owns it, which is also the first moment it can be told which world it
// is in. Placement builds the curve in the frame of the block that was clicked second, so the block entity at the other
// end is handed a curve whose own positions were named from across the seam — and the far end of the pair is then
// derived from this one, so folding here settles both copies.
//
// Taken at the head rather than at the store: the map is keyed by the curve's second block position and the guard right
// below compares against that same key, so a key still naming a block past the bounds would file the curve where no
// later lookup goes looking for it.
//
// A block entity restored from disk does not come through here — it fills its map directly, out of deltas already
// measured from itself, so its curves need nothing. What they still lack is the dimension, and the place that supplies
// it is the graph, which is where such a curve is next used.
@Mixin(value = TrackBlockEntity.class, remap = false)
public class TrackBlockEntityMixin {
    @Inject(method = "addConnection", at = @At("HEAD"))
    private void toroidal$foldAddedCurve(BezierConnection connection, CallbackInfo ci) {
        Level level = ((TrackBlockEntity) (Object) this).getLevel();
        ((BezierCurveFold) connection).toroidal$foldCurve(level, level == null ? null : level.dimension());
    }
}
