package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket;
import com.toroidalworld.compat.create.CreateSeamFold;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

@Mixin(value = BlockEntityConfigurationPacket.class, remap = false)
public class BlockEntityConfigurationPacketMixin {
    @Mutable
    @Shadow
    @Final
    protected BlockPos pos;

    @Inject(method = "handle", at = @At("HEAD"))
    private void toroidal$canonicalisePacketPos(ServerPlayer player, CallbackInfo ci) {
        if (player != null) {
            this.pos = CreateSeamFold.canonical(player.serverLevel(), this.pos);
        }
    }
}
