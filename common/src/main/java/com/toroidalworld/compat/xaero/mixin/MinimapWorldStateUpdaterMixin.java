package com.toroidalworld.compat.xaero.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.toroidalworld.compat.xaero.XaeroFold;

import net.minecraft.core.BlockPos;

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
