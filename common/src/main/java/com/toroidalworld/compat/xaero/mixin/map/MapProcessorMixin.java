package com.toroidalworld.compat.xaero.mixin.map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.toroidalworld.compat.xaero.XaeroWorldMapFold;

import net.minecraft.core.BlockPos;

import xaero.map.MapProcessor;

// The world map's "mw" multiworld id is derived from the held world spawn (quantized to 64 blocks), and this
// method is the one point the derivation reads it through — the server-provided level id, when present, bypasses
// the spawn and needs no help. Folded on read for the same reason as the minimap twin: the bounds payload can
// arrive after the spawn packet at login, and a read-time fold converges to the canonical id the moment the
// bounds are known. When the world map is installed, the minimap defers its own id to this one, so this fold
// closes the dedicated-server churn for the pair.
@Mixin(value = MapProcessor.class, remap = false)
public abstract class MapProcessorMixin {
    @Inject(method = "getAutoIdBase", at = @At("RETURN"), cancellable = true)
    private void toroidal$foldIdSpawn(CallbackInfoReturnable<Object> cir) {
        if (cir.getReturnValue() instanceof BlockPos spawn) {
            BlockPos folded = XaeroWorldMapFold.foldIdSpawn(spawn);
            if (folded != spawn) {
                cir.setReturnValue(folded);
            }
        }
    }
}
