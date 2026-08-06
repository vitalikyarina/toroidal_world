package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.commands.SetSpawnCommand;

// A respawn point is settled here, once, before the command does anything with it — because the command does two
// things with it, and they must not be able to disagree. It hands the point to the player's storage and it prints the
// point back to the sender, and a coordinate corrected on the way into storage alone would leave the message naming a
// place the world does not have: fifty blocks north of the seam reads back as the corner of the world it walked past.
//
// This is also the one method both spellings of the command pass through. /spawnpoint with a position resolves it from
// a coordinate argument; /spawnpoint without one reads the sender's own position off the command source and never
// touches an argument at all — so a guard on the argument would miss half the command.
//
// The storage still settles the point again on its way in. That is not this rule repeated: this one decides what the
// sender is told, and the one at the sink decides what the server writes to disk, which has to hold for every producer
// of a respawn point and not only for the two commands that name one out loud.
@Mixin(SetSpawnCommand.class)
public class SetSpawnCommandMixin {
    @ModifyVariable(method = "setSpawn", at = @At("HEAD"), argsOnly = true)
    private static BlockPos toroidal$spawnInsideBounds(BlockPos pos,
            @Local(argsOnly = true) CommandSourceStack source) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(source.getLevel());
        return transformer == null ? pos : transformer.blocks.wrap(pos);
    }
}
