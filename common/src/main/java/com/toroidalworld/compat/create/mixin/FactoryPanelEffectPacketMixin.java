package com.toroidalworld.compat.create.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelEffectPacket;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import com.toroidalworld.compat.create.client.CreateClientFrame;

import net.minecraft.core.BlockPos;

@Mixin(value = FactoryPanelEffectPacket.class, remap = false)
public abstract class FactoryPanelEffectPacketMixin {
    @ModifyArg(method = "handle",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientLevel;getBlockState"
                            + "(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockPos toroidal$sourceBlockInTheViewerFrame(BlockPos canonical) {
        return CreateClientFrame.inViewerFrame(canonical);
    }

    @ModifyExpressionValue(method = "handle",
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
                    target = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelEffectPacket;"
                            + "toPos:Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelPosition;"))
    private FactoryPanelPosition toroidal$targetPanelInTheViewerFrame(FactoryPanelPosition canonical) {
        BlockPos folded = CreateClientFrame.inViewerFrame(canonical.pos());
        return folded == canonical.pos() ? canonical : new FactoryPanelPosition(folded, canonical.slot());
    }
}
