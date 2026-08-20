package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.entity.SeamAim;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Ravager;

@Mixin(Ravager.class)
public class RavagerKnockbackMixin {
    @ModifyVariable(
            method = "strongKnockback(Lnet/minecraft/world/entity/Entity;)V",
            at = @At("STORE"), ordinal = 0)
    private double toroidal$shoveDirectionX(double deltaX) {
        return SeamAim.foldX((Entity) (Object) this, deltaX);
    }

    @ModifyVariable(
            method = "strongKnockback(Lnet/minecraft/world/entity/Entity;)V",
            at = @At("STORE"), ordinal = 1)
    private double toroidal$shoveDirectionZ(double deltaZ) {
        return SeamAim.foldZ((Entity) (Object) this, deltaZ);
    }
}
