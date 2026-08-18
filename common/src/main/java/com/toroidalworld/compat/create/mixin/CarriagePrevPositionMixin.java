package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.toroidalworld.compat.create.CreateTrackFold;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

// Where the carriage stood last tick — the whole of what the contraption knows about its own motion. On the server it
// is the position stamped just before the anchor is written onto the entity, so on the tick the carriage is renamed it
// names the side of the world the carriage has just left, and the difference between it and the current position is a
// whole world rather than the step the train actually took.
//
// Everything downstream reads that difference as motion: the collider carries what stands on the deck by it, the
// contact-point motion pushes them along by it, the run-over damage is its length times sixteen, and the previous
// anchor the collision geometry is interpolated from is this very vector. So the fold goes here, on the one method they
// all ask, rather than on each of them — the previous position is brought to the copy nearest the current one, which is
// the step the train took. Wherever no rename happened the nearest copy is the argument itself and every consumer keeps
// reading Create's own arithmetic untouched.
@Mixin(targets = "com.simibubi.create.content.trains.entity.CarriageContraptionEntity", remap = false)
public abstract class CarriagePrevPositionMixin {
    @ModifyReturnValue(method = "getPrevPositionVec", at = @At("RETURN"))
    private Vec3 toroidal$prevPositionInCurrentFrame(Vec3 previous) {
        Entity carriage = (Entity) (Object) this;
        return CreateTrackFold.nearestCopy(carriage.level(), carriage.position(), previous);
    }
}
