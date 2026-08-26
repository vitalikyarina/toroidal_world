package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.PlaySoundCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

@Mixin(PlaySoundCommand.class)
public class PlaySoundCommandMixin {
    @WrapOperation(
            method = "playSound",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;getX()D"))
    private static double toroidal$listenerXNearestTheSound(ServerPlayer listener, Operation<Double> original,
            @Local(argsOnly = true) CommandSourceStack source, @Local(argsOnly = true) Vec3 position) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(source.getLevel());
        return transformer == null
                ? original.call(listener)
                : transformer.nearestCopy(position, listener.position()).x;
    }

    @WrapOperation(
            method = "playSound",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;getZ()D"))
    private static double toroidal$listenerZNearestTheSound(ServerPlayer listener, Operation<Double> original,
            @Local(argsOnly = true) CommandSourceStack source, @Local(argsOnly = true) Vec3 position) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(source.getLevel());
        return transformer == null
                ? original.call(listener)
                : transformer.nearestCopy(position, listener.position()).z;
    }
}
