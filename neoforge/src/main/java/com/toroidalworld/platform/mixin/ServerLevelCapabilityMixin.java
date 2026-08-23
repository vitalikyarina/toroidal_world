package com.toroidalworld.platform.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

@Mixin(ServerLevel.class)
public class ServerLevelCapabilityMixin {
    @ModifyVariable(method = "registerCapabilityListener", at = @At("HEAD"), argsOnly = true)
    private BlockPos toroidal$registerUnderCanonicalKey(BlockPos pos) {
        return toroidal$canonicalKey(pos);
    }

    @ModifyVariable(method = "invalidateCapabilities(Lnet/minecraft/core/BlockPos;)V", at = @At("HEAD"), argsOnly = true)
    private BlockPos toroidal$invalidateUnderCanonicalKey(BlockPos pos) {
        return toroidal$canonicalKey(pos);
    }

    @Unique
    private BlockPos toroidal$canonicalKey(BlockPos pos) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf((ServerLevel) (Object) this);
        return transformer == null ? pos : transformer.blocks.wrap(pos);
    }
}
