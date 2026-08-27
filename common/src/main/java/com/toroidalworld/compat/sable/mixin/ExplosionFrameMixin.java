package com.toroidalworld.compat.sable.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.toroidalworld.compat.sable.SeamFrame;

import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(Explosion.class)
public abstract class ExplosionFrameMixin {
    @Shadow
    @Final
    private Level level;

    @Shadow
    public abstract Vec3 center();

    @WrapMethod(method = "explode")
    private void toroidal$frameOnExplosion(Operation<Void> original) {
        Vec3 centre = this.center();
        SeamFrame.run(this.level, () -> centre, original::call);
    }
}
