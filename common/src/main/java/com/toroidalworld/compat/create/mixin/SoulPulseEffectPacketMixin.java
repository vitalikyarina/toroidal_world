package com.toroidalworld.compat.create.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.equipment.bell.SoulPulseEffectPacket;
import com.toroidalworld.compat.create.client.CreateClientFrame;

import net.minecraft.core.BlockPos;

@Mixin(value = SoulPulseEffectPacket.class, remap = false)
public abstract class SoulPulseEffectPacketMixin {
    @ModifyExpressionValue(method = "handle",
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
                    target = "Lcom/simibubi/create/content/equipment/bell/SoulPulseEffectPacket;pos:Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$pulseCentreInTheViewerFrame(BlockPos canonical) {
        return CreateClientFrame.inViewerFrame(canonical);
    }
}
