package com.toroidalworld.compat.sable.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.toroidalworld.compat.sable.SeamFrame;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(Entity.class)
public abstract class EntityFrameMixin {
    @Shadow
    public abstract Level level();

    @Shadow
    public abstract Vec3 position();

    @WrapMethod(method = "getOnPos(F)Lnet/minecraft/core/BlockPos;")
    private BlockPos toroidal$frameOnFootQuery(float distance, Operation<BlockPos> original) {
        return SeamFrame.with(this.level(), this::position, () -> original.call(distance));
    }
}
