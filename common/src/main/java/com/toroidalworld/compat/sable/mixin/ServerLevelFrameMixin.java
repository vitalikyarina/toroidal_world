package com.toroidalworld.compat.sable.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.toroidalworld.compat.sable.SeamFrame;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

@Mixin(ServerLevel.class)
public abstract class ServerLevelFrameMixin {
    @WrapMethod(method = "tickNonPassenger")
    private void toroidal$frameOnEntityTick(Entity entity, Operation<Void> original) {
        SeamFrame.run((ServerLevel) (Object) this, entity::position, () -> original.call(entity));
    }

    @WrapMethod(method = "tickPassenger")
    private void toroidal$frameOnPassengerTick(Entity vehicle, Entity passenger, Operation<Void> original) {
        SeamFrame.run((ServerLevel) (Object) this, passenger::position, () -> original.call(vehicle, passenger));
    }
}
