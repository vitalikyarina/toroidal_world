package com.toroidalworld.compat.c2me.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.toroidalworld.compat.c2me.C2meFoldedNoiseDotEmitter;
import com.toroidalworld.compat.c2me.C2meFoldedNoiseNode;
import com.ishland.c2me.opts.dfc.common.gen.dot.DotGenRegistry;

// The graph half of the same registration. C2ME draws every compiled function whether anyone asked for it or not, and
// its registry throws on an unknown node class, so the drawing is as required as the bytecode.
@Mixin(DotGenRegistry.class)
public class DotGenRegistryMixin {
    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void toroidal$registerFoldedNoise(CallbackInfo ci) {
        DotGenRegistry.REGISTRY.registerExactMatch(C2meFoldedNoiseNode.class, C2meFoldedNoiseDotEmitter.INSTANCE);
    }
}
