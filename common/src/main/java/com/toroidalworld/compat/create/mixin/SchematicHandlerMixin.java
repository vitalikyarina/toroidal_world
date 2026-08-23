package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.schematics.client.SchematicHandler;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.toroidalworld.compat.create.CreateSchematicFold;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

@Mixin(value = SchematicHandler.class, remap = false)
public class SchematicHandlerMixin {
    @Unique
    private BlockPos toroidal$lastScannedKnownPos;

    @WrapOperation(
            method = "fixControllerBlockEntities",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/foundation/blockEntity/IMultiBlockEntityContainer;"
                            + "getLastKnownPos()Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$rememberLastKnownPos(IMultiBlockEntityContainer container,
            Operation<BlockPos> original) {
        BlockPos lastKnown = original.call(container);
        toroidal$lastScannedKnownPos = lastKnown;
        return lastKnown;
    }

    @WrapOperation(
            method = "fixControllerBlockEntities",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/foundation/blockEntity/IMultiBlockEntityContainer;"
                            + "getController()Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$foldScannedController(IMultiBlockEntityContainer container,
            Operation<BlockPos> original) {
        BlockPos controller = original.call(container);
        BlockPos lastKnown = toroidal$lastScannedKnownPos;
        if (controller == null || lastKnown == null) {
            return controller;
        }

        return CreateSchematicFold.scannedControllerNear(Minecraft.getInstance().level, lastKnown, controller);
    }
}
