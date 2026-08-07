package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.accessors.TransformerHolder;
import com.toroidalworld.core.WorldLoopTransformer;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.Direction;
import net.minecraft.world.level.lighting.BlockLightEngine;

// Block light propagates neighbour by neighbour: each step offsets the current node one block and stores light there. At
// the seam that offset lands a block past the bounds, in a section the engine never stores, so propagation stops dead
// and a torch on the edge leaves the other side dark. Wrapping the target node back into the world lets the same step
// reach the real section across the seam — and, because every store that follows uses that wrapped node, the section
// graph itself stays in bounds and untouched.
//
// The transformer comes from LightEngineMixin, the shared base of both engines, rather than being resolved and cached
// again here.
@Mixin(BlockLightEngine.class)
public abstract class BlockLightEngineMixin {
    @WrapOperation(
            method = {"propagateIncrease", "propagateDecrease"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;offset(JLnet/minecraft/core/Direction;)J"),
            expect = 2)
    private long toroidal$wrapToNode(long node, Direction direction, Operation<Long> original) {
        long toNode = original.call(node, direction);
        WorldLoopTransformer transformer = ((TransformerHolder) (Object) this).toroidal$transformer();
        return transformer.isWrapped() ? transformer.wrapBlockNode(toNode) : toNode;
    }
}
