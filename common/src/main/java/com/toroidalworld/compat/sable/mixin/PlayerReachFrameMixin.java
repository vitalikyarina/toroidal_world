package com.toroidalworld.compat.sable.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.toroidalworld.compat.sable.SableReachFrame;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

@Mixin(Player.class)
public abstract class PlayerReachFrameMixin {
    @WrapMethod(method = "canInteractWithBlock")
    private boolean toroidal$frameOnBlockReach(BlockPos pos, double buffer, Operation<Boolean> original) {
        return SableReachFrame.blockReach((Player) (Object) this, pos, buffer, original);
    }

    @WrapMethod(method = "canInteractWithEntity(Lnet/minecraft/world/phys/AABB;D)Z")
    private boolean toroidal$frameOnEntityReach(AABB box, double buffer, Operation<Boolean> original) {
        return SableReachFrame.entityReach((Player) (Object) this, box, buffer, original);
    }
}
