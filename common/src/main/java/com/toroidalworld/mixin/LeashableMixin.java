package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;

@Mixin(Leashable.class)
public interface LeashableMixin {
    @WrapMethod(method = "elasticRangeLeashBehaviour")
    private void toroidal$elasticPullThroughSeam(Entity leashHolder, float leashDistance, Operation<Void> original) {
        Entity leashed = (Entity) this;
        double holderX = SeamAim.nearX(leashed, leashHolder.getX());
        double holderZ = SeamAim.nearZ(leashed, leashHolder.getZ());
        if (holderX == leashHolder.getX() && holderZ == leashHolder.getZ()) {
            original.call(leashHolder, leashDistance);
            return;
        }

        double pullX = (holderX - leashed.getX()) / leashDistance;
        double pullY = (leashHolder.getY() - leashed.getY()) / leashDistance;
        double pullZ = (holderZ - leashed.getZ()) / leashDistance;
        leashed.setDeltaMovement(
                leashed.getDeltaMovement()
                        .add(Math.copySign(pullX * pullX * 0.4, pullX), Math.copySign(pullY * pullY * 0.4, pullY),
                                Math.copySign(pullZ * pullZ * 0.4, pullZ)));
    }
}
