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

// Only the source region is refused. It is named by two corners and is therefore ambiguous across the seam like any
// /fill. The destination is not: it is an anchor plus the size of what is being copied, which describes exactly one
// place whether or not it lies across the seam — refusing that would forbid something nobody could misread. What that
// one place costs instead is the overlap test, which has to be measured where the destination really lies.
//
// The refusal sits at the first use of the finished locals, before the overlap and volume checks, so a region that is
// both ambiguous and oversized is answered with the reason that actually stops it. The source dimension is read rather
// than the sender's: /clone from <dimension> copies out of a level the command was not run in.
//
// Both handlers take their arguments through @Local rather than declaring them ahead of the callback, as the rest of
// the mod's injections do. Declaring them would mean naming CloneCommands$DimensionAndPosition, which is private, and
// opening a vanilla type for the sake of how a parameter list looks is a poor trade.
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

    // The overlap a mode forbids is a physical one, and vanilla tests it on the raw corners. The destination is the one
    // region here allowed to run past the bounds — it is an anchor plus the size of the copy, so it is never refused —
    // and past them it lies against the far edge of the world: a source standing there shares blocks with it in fact,
    // while the two boxes read a world apart and the copy proceeds to read what it is overwriting.
    //
    // Only the source dimension is asked, because vanilla has already established the two are the same one: the test
    // sits behind fromDimension == toDimension, and Java stops there when they differ.
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
