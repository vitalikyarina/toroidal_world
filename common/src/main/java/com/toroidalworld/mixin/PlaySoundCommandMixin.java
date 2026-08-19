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
