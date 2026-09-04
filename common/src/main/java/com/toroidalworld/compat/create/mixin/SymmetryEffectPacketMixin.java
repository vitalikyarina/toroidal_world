package com.toroidalworld.compat.create.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.equipment.symmetryWand.SymmetryEffectPacket;
import com.toroidalworld.compat.create.client.CreateClientFrame;

import net.minecraft.core.BlockPos;

@Mixin(value = SymmetryEffectPacket.class, remap = false)
public abstract class SymmetryEffectPacketMixin {
    @ModifyExpressionValue(method = "handle",
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
                    target = "Lcom/simibubi/create/content/equipment/symmetryWand/SymmetryEffectPacket;"
                            + "mirror:Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$mirrorInTheViewerFrame(BlockPos canonical) {
        return CreateClientFrame.inViewerFrame(canonical);
    }

    @WrapOperation(method = "handle",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/equipment/symmetryWand/SymmetryHandler;"
                            + "drawEffect(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)V"))
    private void toroidal$drawTheSegmentInOneFrame(BlockPos mirror, BlockPos placed, Operation<Void> original) {
        original.call(mirror, CreateClientFrame.nearestCopy(mirror, placed));
    }
}
