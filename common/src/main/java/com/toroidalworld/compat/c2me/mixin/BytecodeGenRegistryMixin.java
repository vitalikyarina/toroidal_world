package com.toroidalworld.compat.c2me.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.toroidalworld.compat.c2me.C2meFoldedNoiseEmitter;
import com.toroidalworld.compat.c2me.C2meFoldedNoiseNode;
import com.ishland.c2me.opts.dfc.common.gen.jvm.BytecodeGenRegistry;

// Registered from C2ME's own static initialiser: the registry freezes on first read, and a later arrival is rejected.
@Mixin(BytecodeGenRegistry.class)
public class BytecodeGenRegistryMixin {
    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void toroidal$registerFoldedNoise(CallbackInfo ci) {
        BytecodeGenRegistry.REGISTRY.registerExactMatch(C2meFoldedNoiseNode.class, C2meFoldedNoiseEmitter.INSTANCE);
    }
}
