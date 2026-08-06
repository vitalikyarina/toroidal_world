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

// The one moment a world's shape can be applied: `onCreate` takes the dimensions the chosen world type produced and
// bakes them into the world. Doing it here, and only here, is what lets the shape be plain screen state — picking a
// world type rebuilds the dimensions from its preset, so anything applied earlier would be quietly overwritten, while
// what is applied here is what the world is actually built from.
@Mixin(CreateWorldScreen.class)
public class CreateWorldScreenMixin {
    // Opening the screen is the moment to forget the last world's choices — not building the tab that shows them.
    // The World tab is rebuilt by `init()`, which also runs on every resize, so resetting there would wipe a shape and
    // a size the player had already picked the moment they resized the window. The constructor runs once per opening.
    //
    // Re-create reuses this same constructor, with the source world's dimensions in the `settings` argument. So right
    // after wiping the last world's choices, the shapes are offered those dimensions: a re-created looped world is
    // recognised and the screen opens on its real shape, while a fresh or normal world is claimed by nobody and stays
    // the default. The tab is built later, in init(), so it reads whatever shape this leaves selected.
    //
    // The argument, not `getUiState().getSettings()`: the ui state rebuilds the dimensions from the preset it derives,
    // and a LoopedChunkGenerator resolves to the Normal preset (it extends the vanilla noise generator), so by the time
    // the constructor tails the looped generator has already been replaced. The argument is the original context
    // untouched — the ui state's rewrite lands on its own copy, since WorldCreationContext is an immutable record.
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
