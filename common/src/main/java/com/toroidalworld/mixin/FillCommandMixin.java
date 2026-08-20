package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.toroidalworld.command.SeamCommandErrors;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.FillCommand;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

@Mixin(FillCommand.class)
public class FillCommandMixin {
    @Inject(method = "fillBlocks", at = @At("HEAD"))
    private static void toroidal$refuseRegionAcrossSeam(CallbackInfoReturnable<Integer> cir,
            @Local(argsOnly = true) CommandSourceStack source,
            @Local(argsOnly = true) BoundingBox region) throws CommandSyntaxException {
        SeamCommandErrors.requireUnambiguousRegion(
                WorldLoopAttachments.wrappedTransformerOf(source.getLevel()), region);
    }
}
