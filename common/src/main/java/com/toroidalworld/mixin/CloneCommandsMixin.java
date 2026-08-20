package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.toroidalworld.command.SeamCommandErrors;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.server.commands.CloneCommands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

@Mixin(CloneCommands.class)
public class CloneCommandsMixin {
    @Inject(
            method = "clone",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/commands/CloneCommands$Mode;canOverlap()Z"))
    private static void toroidal$refuseSourceAcrossSeam(CallbackInfoReturnable<Integer> cir,
            @Local(ordinal = 0) BoundingBox from,
            @Local(ordinal = 0) ServerLevel fromDimension) throws CommandSyntaxException {
        SeamCommandErrors.requireUnambiguousRegion(
                WorldLoopAttachments.wrappedTransformerOf(fromDimension), from);
    }

    @WrapOperation(
            method = "clone",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/structure/BoundingBox;intersects(Lnet/minecraft/world/level/levelgen/structure/BoundingBox;)Z"))
    private static boolean toroidal$measureOverlapThroughSeam(BoundingBox destination, BoundingBox from,
            Operation<Boolean> original, @Local(ordinal = 0) ServerLevel fromDimension) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(fromDimension);
        return transformer == null
                ? original.call(destination, from)
                : transformer.regionsOverlap(destination, from);
    }
}
