package com.toroidalworld.compat.xaero.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.toroidalworld.compat.xaero.XaeroFold;

import net.minecraft.core.BlockPos;

// Xaero's multiplayer waypoint-store id is "mw" + the held world spawn quantized to 64 blocks, and this method is
// the one point the derivation reads that spawn through (a server-provided level id, when present, bypasses the
// spawn and needs no help). Folded on read rather than where the spawn packet stores it: the bounds payload can
// arrive after the spawn packet at login, and a read-time fold converges to the canonical id the moment the bounds
// are known instead of keeping whatever copy happened to be stored first.
@Mixin(targets = "xaero.hud.minimap.world.state.MinimapWorldStateUpdater", remap = false)
public abstract class MinimapWorldStateUpdaterMixin {
    @Inject(method = "getAutoWorldNodeBase", at = @At("RETURN"), cancellable = true)
    private void toroidal$foldSpawnNodeBase(CallbackInfoReturnable<Object> cir) {
        if (cir.getReturnValue() instanceof BlockPos spawn) {
            BlockPos folded = XaeroFold.foldWorldNodeSpawn(spawn);
            if (folded != spawn) {
                cir.setReturnValue(folded);
            }
        }
    }
}
