package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnection;
import com.toroidalworld.compat.create.CreateInvokeTargets;
import com.toroidalworld.compat.create.client.CreateClientFrame;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

@Mixin(value = FactoryPanelConnection.class, remap = false)
public class FactoryPanelConnectionMixin {
    @WrapOperation(method = "calculatePathDiff",
            at = @At(value = "INVOKE",
                    target = CreateInvokeTargets.BLOCK_POS_SUBTRACT))
    private BlockPos toroidal$foldStoredSource(BlockPos target, Vec3i source, Operation<BlockPos> original) {
        if (!(source instanceof BlockPos stored)) {
            return original.call(target, source);
        }

        return original.call(target, CreateClientFrame.nearestCopy(Minecraft.getInstance().level, stored));
    }
}
