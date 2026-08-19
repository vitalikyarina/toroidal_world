package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.toroidalworld.command.SeamCommandErrors;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.WorldCoordinate;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.world.phys.Vec3;

@Mixin(WorldCoordinates.class)
public class CommandCoordinatesMixin {
    @Shadow
    @Final
    private WorldCoordinate x;

    @Shadow
    @Final
    private WorldCoordinate z;

    @Inject(method = "getPosition", at = @At("HEAD"))
    private void toroidal$refuseCoordinateOutsideWorld(CommandSourceStack source, CallbackInfoReturnable<Vec3> cir)
            throws CommandSyntaxException {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(source.getLevel());
        if (transformer == null) {
            return;
        }

        SeamCommandErrors.requireInsideWorld(transformer.coords.x, this.x);
        SeamCommandErrors.requireInsideWorld(transformer.coords.z, this.z);
    }
}
