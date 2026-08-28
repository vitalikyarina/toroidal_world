package com.toroidalworld.compat.sable.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.toroidalworld.compat.sable.SableReachFrame;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerReachFrameMixin {
    @WrapMethod(method = "isReachableBedBlock")
    private boolean toroidal$frameOnBedReach(BlockPos pos, Operation<Boolean> original) {
        return SableReachFrame.bedReach((ServerPlayer) (Object) this, pos, original);
    }
}
