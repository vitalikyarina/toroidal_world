package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.gearbox.GearboxBlockEntity;
import com.simibubi.create.content.kinetics.gearbox.GearboxRenderer;
import com.toroidalworld.compat.create.CreateInvokeTargets;
import com.toroidalworld.compat.create.CreateSeamFold;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

// The same fold as GearboxVisualMixin, on the path Create takes where the backend supports no visualization. Both of
// them exist because the two renderers spell the subtraction out separately — whichever one the player's backend
// picks, the gearbox has to draw its half shafts the same way.
//
// Named by descriptor: the generic renderSafe leaves two bridge methods of the same name behind it. The whole
// parameter list is captured because the block entity is the first of them, and it is the only thing here that knows
// the level and the anchor.
@Mixin(value = GearboxRenderer.class, remap = false)
public class GearboxRendererMixin {
    @WrapOperation(
            method = "renderSafe(Lcom/simibubi/create/content/kinetics/gearbox/GearboxBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At(value = "INVOKE",
                    target = CreateInvokeTargets.BLOCK_POS_SUBTRACT))
    private BlockPos toroidal$foldSourceDelta(BlockPos sourcePos, Vec3i anchorPos, Operation<BlockPos> original,
            GearboxBlockEntity be, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light,
            int overlay) {
        return CreateSeamFold.foldDelta(be.getLevel(), be.getBlockPos(), sourcePos,
                original.call(sourcePos, anchorPos));
    }
}
