package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
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
    @WrapOperation(
            method = "fixControllerBlockEntities",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/foundation/blockEntity/IMultiBlockEntityContainer;"
                            + "getController()Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$foldScannedController(IMultiBlockEntityContainer container,
            Operation<BlockPos> original) {
        BlockPos controller = original.call(container);
        BlockPos lastKnown = container.getLastKnownPos();
        if (controller == null || lastKnown == null) {
            return controller;
        }

        return CreateSchematicFold.scannedControllerNear(Minecraft.getInstance().level, lastKnown, controller);
    }
}
