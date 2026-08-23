package com.toroidalworld.compat.create.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelEffectPacket;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import com.toroidalworld.compat.create.client.CreateClientFrame;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

@Mixin(value = FactoryPanelEffectPacket.class, remap = false)
public abstract class FactoryPanelEffectPacketMixin {
    @ModifyExpressionValue(method = "handle",
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
                    target = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelEffectPacket;"
                            + "fromPos:Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelPosition;"))
    private FactoryPanelPosition toroidal$sourcePanelInTheViewerFrame(FactoryPanelPosition canonical) {
        return inViewerFrame(canonical);
    }

    @ModifyExpressionValue(method = "handle",
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
                    target = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelEffectPacket;"
                            + "toPos:Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelPosition;"))
    private FactoryPanelPosition toroidal$targetPanelInTheViewerFrame(FactoryPanelPosition canonical) {
        return inViewerFrame(canonical);
    }

    private static FactoryPanelPosition inViewerFrame(FactoryPanelPosition canonical) {
        BlockPos folded = CreateClientFrame.nearestCopy(Minecraft.getInstance().level, canonical.pos());
        return folded == canonical.pos() ? canonical : new FactoryPanelPosition(folded, canonical.slot());
    }
}
