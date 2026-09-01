package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.commands.SetWorldSpawnCommand;

@Mixin(SetWorldSpawnCommand.class)
public class SetWorldSpawnCommandMixin {
    @ModifyVariable(method = "setSpawn", at = @At("HEAD"), argsOnly = true)
    private static BlockPos toroidal$worldSpawnInsideBounds(BlockPos pos,
            @Local(argsOnly = true) CommandSourceStack source) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(source.getLevel());
        return transformer == null ? pos : transformer.fold(pos);
    }
}
