package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.accessors.TransformerHolder;
import com.toroidalworld.core.WorldLoopTransformer;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.Direction;
import net.minecraft.world.level.lighting.BlockLightEngine;

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
