package com.toroidalworld.compat.xaero.mixin.map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.toroidalworld.compat.xaero.XaeroWorldMapFold;

import net.minecraft.core.BlockPos;

import xaero.map.MapProcessor;

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
