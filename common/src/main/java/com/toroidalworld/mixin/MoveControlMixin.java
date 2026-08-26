package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.toroidalworld.accessors.NavigationShifter;
import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldFold;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.Vec3;

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
        WorldFold transformer = ((TransformerSource) this.mob).toroidal$wrappedTransformer();
        if (transformer == null) {
            original.call(x, y, z, speedModifier);
            return;
        }

        Vec3 nearest = transformer.nearestCopy(this.mob.position(), new Vec3(x, y, z));
        original.call(nearest.x, nearest.y, nearest.z, speedModifier);
    }

    @Override
    public void toroidal$shiftBy(int shiftX, int shiftZ) {
        this.wantedX += shiftX;
        this.wantedZ += shiftZ;
    }
}
