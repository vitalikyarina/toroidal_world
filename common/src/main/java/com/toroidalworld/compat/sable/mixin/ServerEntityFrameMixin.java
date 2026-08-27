package com.toroidalworld.compat.sable.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.toroidalworld.compat.sable.SeamFrame;

import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

@Mixin(ServerEntity.class)
public abstract class ServerEntityFrameMixin {
    @Shadow
    @Final
    private ServerLevel level;

    @Shadow
    @Final
    private Entity entity;

    @WrapMethod(method = "sendChanges")
    private void toroidal$frameOnTracking(Operation<Void> original) {
        SeamFrame.run(this.level, this.entity::position, original::call);
    }
}
