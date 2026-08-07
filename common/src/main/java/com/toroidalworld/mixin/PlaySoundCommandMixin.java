package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.PlaySoundCommand;
import net.minecraft.world.phys.Vec3;

// The command measures each listener itself instead of going through PlayerList.broadcast, so the seam-aware distance
// that covers every other sound in the game never touches it: across the seam the gap reads as a whole world, the
// listener falls outside the sound's range and is either skipped or, with a minimum volume set, told the sound is far
// behind them when it is a few blocks ahead.
//
// Rather than correct the gap after it is measured, the listener is placed where the sound can see them: at the copy of
// themselves nearest the source. Both horizontal readings then follow — the range test, and the nearby point a distant
// sound is pulled to, which the command builds from the listener's own coordinates. The vertical one has nowhere to
// fold, so getY is left alone.
//
// A listener answering from beyond the bounds is what the packet translation is for: it lays every position back into
// the client's own space around that client's mirror, and a coordinate a world out arrives there like any other.
@Mixin(PlaySoundCommand.class)
public class PlaySoundCommandMixin {
    @ModifyExpressionValue(
            method = "playSound",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;getX()D"))
    private static double toroidal$listenerXNearestTheSound(double listenerX,
            @Local(argsOnly = true) CommandSourceStack source, @Local(argsOnly = true) Vec3 position) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(source.getLevel());
        return transformer == null ? listenerX : transformer.coords.x.unwrapAround(position.x, listenerX);
    }

    @ModifyExpressionValue(
            method = "playSound",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;getZ()D"))
    private static double toroidal$listenerZNearestTheSound(double listenerZ,
            @Local(argsOnly = true) CommandSourceStack source, @Local(argsOnly = true) Vec3 position) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(source.getLevel());
        return transformer == null ? listenerZ : transformer.coords.z.unwrapAround(position.z, listenerZ);
    }
}
