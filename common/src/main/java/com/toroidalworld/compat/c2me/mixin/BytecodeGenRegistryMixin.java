package com.toroidalworld.compat.c2me.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.toroidalworld.compat.c2me.C2meFoldedNoiseEmitter;
import com.toroidalworld.compat.c2me.C2meFoldedNoiseNode;
import com.ishland.c2me.opts.dfc.common.gen.jvm.BytecodeGenRegistry;

// Registers from inside C2ME's own static initialiser rather than from this mod's entry point, because the registry
// freezes the first time it is read and the first read is the first compilation — anything that arrives later is
// rejected, and a node with no emitter is a compilation that throws.
@Mixin(BytecodeGenRegistry.class)
public class BytecodeGenRegistryMixin {
    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void toroidal$registerFoldedNoise(CallbackInfo ci) {
        BytecodeGenRegistry.REGISTRY.registerExactMatch(C2meFoldedNoiseNode.class, C2meFoldedNoiseEmitter.INSTANCE);
    }
}
