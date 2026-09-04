package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.toroidalworld.command.SeamCommandErrors;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

@Mixin(WorldCoordinates.class)
public class CommandCoordinatesMixin {
    @Inject(method = "getPosition", at = @At("HEAD"))
    private void toroidal$refuseCoordinateOutsideWorld(CommandSourceStack source, CallbackInfoReturnable<Vec3> cir)
            throws CommandSyntaxException {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(source.getLevel());
        if (transformer == null) {
            return;
        }

        WorldCoordinates self = (WorldCoordinates) (Object) this;
        SeamCommandErrors.requireInsideWorld(transformer, Direction.Axis.X, self.x());
        SeamCommandErrors.requireInsideWorld(transformer, Direction.Axis.Z, self.z());
    }
}
