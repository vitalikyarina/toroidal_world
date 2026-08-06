package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.toroidalworld.accessors.NavigationShifter;
import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldLoopTransformer;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.Vec3;

// Knowing how far the target is does not tell a mob which way to walk. The direction is worked out from the absolute
// positions, and across the seam that points the long way round the world — a chicken that correctly sees you two steps
// away sets off in the opposite direction. The target is unwrapped around the mob, so it becomes the copy next door.
@Mixin(MoveControl.class)
public class MoveControlMixin implements NavigationShifter {
    @Shadow
    @Final
    protected Mob mob;

    @Shadow
    protected double wantedX;

    @Shadow
    protected double wantedZ;

    @WrapMethod(method = "setWantedPosition")
    private void toroidal$wantedPositionThroughSeam(double x, double y, double z, double speedModifier,
            Operation<Void> original) {
        WorldLoopTransformer transformer = ((TransformerSource) this.mob).toroidal$wrappedTransformer();
        if (transformer == null) {
            original.call(x, y, z, speedModifier);
            return;
        }

        Vec3 nearest = transformer.vectors.nearestCopy(this.mob.position(), new Vec3(x, y, z));
        original.call(nearest.x, nearest.y, nearest.z, speedModifier);
    }

    // A pending wanted point is consumed by tick() as a plain difference one tick later — after a wrap that is a
    // world-away turn and a step back across the line. The wrap funnel shifts it with the mob (see NavigationShifter).
    @Override
    public void toroidal$shiftBy(int shiftX, int shiftZ) {
        this.wantedX += shiftX;
        this.wantedZ += shiftZ;
    }
}
