package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmPlacementPacket;
import com.toroidalworld.compat.create.client.CreateClientFrame;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

@Mixin(value = ArmPlacementPacket.ClientBoundRequest.class, remap = false)
public class ArmPlacementRequestMixin {
    @ModifyArg(method = "handle",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/kinetics/mechanicalArm/ArmInteractionPointHandler;flushSettings(Lnet/minecraft/core/BlockPos;)V"))
    private BlockPos toroidal$foldPlacementEcho(BlockPos placed) {
        return CreateClientFrame.nearestCopy(Minecraft.getInstance().level, placed);
    }
}
