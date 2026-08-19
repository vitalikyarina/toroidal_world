package com.toroidalworld.client.shape.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.toroidalworld.client.shape.WorldShapes;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.world.level.levelgen.WorldDimensions;

@Mixin(CreateWorldScreen.class)
public class CreateWorldScreenMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void toroidal$resetCreationOptions(CallbackInfo ci, @Local(argsOnly = true) WorldCreationContext settings) {
        WorldShapes.resetToDefault();
        WorldShapes.restoreFromExisting(settings.worldgenLoadContext(), settings.selectedDimensions());
    }

    @WrapOperation(
            method = "onCreate",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/worldselection/WorldCreationContext;selectedDimensions()Lnet/minecraft/world/level/levelgen/WorldDimensions;"))
    private WorldDimensions toroidal$shapeDimensions(WorldCreationContext context, Operation<WorldDimensions> original) {
        return WorldShapes.applyAtCreation(context.worldgenLoadContext(), original.call(context));
    }
}
