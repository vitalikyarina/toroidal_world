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
import net.minecraft.world.entity.ai.control.LookControl;

@Mixin(LookControl.class)
public class LookControlMixin implements NavigationShifter {
    @Shadow
    @Final
    protected Mob mob;

    @Shadow
    protected double wantedX;

    @Shadow
    protected double wantedZ;

    @WrapMethod(method = "setLookAt(DDDFF)V")
    private void toroidal$lookThroughSeam(double x, double y, double z, float yMaxRotSpeed, float xMaxRotAngle,
            Operation<Void> original) {
        WorldLoopTransformer transformer = ((TransformerSource) this.mob).toroidal$wrappedTransformer();
        if (transformer == null) {
            original.call(x, y, z, yMaxRotSpeed, xMaxRotAngle);
            return;
        }

        double nearestX = transformer.coords.x.unwrapAround(this.mob.getX(), x);
        double nearestZ = transformer.coords.z.unwrapAround(this.mob.getZ(), z);
        original.call(nearestX, y, nearestZ, yMaxRotSpeed, xMaxRotAngle);
    }

    @Override
    public void toroidal$shiftBy(int shiftX, int shiftZ) {
        this.wantedX += shiftX;
        this.wantedZ += shiftZ;
    }
}
