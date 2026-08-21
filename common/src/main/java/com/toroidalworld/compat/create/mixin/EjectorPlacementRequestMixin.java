package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.simibubi.create.content.logistics.depot.EjectorPlacementPacket;
import com.toroidalworld.compat.create.client.CreateClientFrame;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

@Mixin(value = EjectorPlacementPacket.ClientBoundRequest.class, remap = false)
public class EjectorPlacementRequestMixin {
    @ModifyArg(method = "handle",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/logistics/depot/EjectorTargetHandler;flushSettings(Lnet/minecraft/core/BlockPos;)V"))
    private BlockPos toroidal$foldPlacementEcho(BlockPos placed) {
        return CreateClientFrame.nearestCopy(Minecraft.getInstance().level, placed);
    }
}
