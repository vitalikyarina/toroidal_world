package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.commands.SetWorldSpawnCommand;

// The world spawn, settled where /spawnpoint settles a player's, and for the same reason: one point reaches both the
// level's storage and the message printed back, and the two must be the same number. This one also has a spelling that
// names no coordinate at all, reading the sender's position straight off the command source, so the whole of it comes
// through this single method.
@Mixin(SetWorldSpawnCommand.class)
public class SetWorldSpawnCommandMixin {
    @ModifyVariable(method = "setSpawn", at = @At("HEAD"), argsOnly = true)
    private static BlockPos toroidal$worldSpawnInsideBounds(BlockPos pos,
            @Local(argsOnly = true) CommandSourceStack source) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(source.getLevel());
        return transformer == null ? pos : transformer.blocks.wrap(pos);
    }
}
