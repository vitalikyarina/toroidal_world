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

// Both corners have already been wrapped into the world by the time they get here, so a region straddling the seam
// arrives as its own complement: thirteen blocks asked for, five hundred filled. The pair is read inside the command
// tree's lambdas, where a scoped handler matches nothing, and BoundingBox.fromCorners is a static with no level to
// ask — so the region is judged where it is finally used, which is also the last moment before anything is written.
//
// Arguments come through @Local rather than being declared ahead of the callback: fillBlocks takes FillCommand$Mode,
// which is private, and opening a vanilla type to change how a parameter list looks is a poor trade.
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
