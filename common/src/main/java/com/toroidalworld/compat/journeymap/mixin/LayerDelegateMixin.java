package com.toroidalworld.compat.journeymap.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.toroidalworld.compat.journeymap.JourneyMapFold;

import net.minecraft.core.BlockPos;

// The one funnel that turns a fullscreen mouse position into a block — hover info, click actions and waypoint
// creation all read it. Over a wrapped copy the raw math names ground past the world bounds; folded here, any copy
// answers with the canonical block it shows.
@Mixin(targets = "journeymap.client.ui.fullscreen.layer.LayerDelegate", remap = false)
public class LayerDelegateMixin {
    @Inject(method = "getBlockPos", at = @At("RETURN"), cancellable = true)
    private void toroidal$foldMouseBlock(CallbackInfoReturnable<BlockPos> cir) {
        BlockPos folded = JourneyMapFold.foldUiBlock(cir.getReturnValue());
        if (folded != cir.getReturnValue()) {
            cir.setReturnValue(folded);
        }
    }
}
