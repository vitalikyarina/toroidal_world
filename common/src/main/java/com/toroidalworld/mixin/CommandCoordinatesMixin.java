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

// The one place a typed coordinate becomes a place in the world. Every positional command argument resolves here —
// BlockPosArgument, Vec3Argument, Vec2Argument and ColumnPosArgument all end at Coordinates.getPosition — so the whole
// command surface takes its meaning from this method alone. What it settles is one question: whether the coordinate
// names a place the world has.
//
// Only a number someone typed out is a claim about where a place is, and on a wrapping axis the world only holds the
// coordinates inside its bounds. That is checked while the coordinate is still the text it was written as — a step
// later it is a Vec3 and the two cases are indistinguishable: 300 typed out and ~50 walked from 250 arrive as the same
// number, and only one of them is a mistake. The parsed form still remembers which, because an axis carries its own
// `relative` flag. A ^local coordinate has nothing to answer for at all — it is a movement from wherever the sender
// stands, never a claim — which is why LocalCoordinates is no longer a target of this mixin.
//
// So a number copied out of the client's raw F3 readout — client space runs whole world widths away from the server's —
// comes back as an error naming the bounds rather than quietly landing somewhere the player did not point at.
//
// Nothing is folded here. A movement that walks out of the world is worth more as it resolved than wrapped: ~-50 from
// z = -254 is fifty blocks north, while its wrapped copy at 208 is a corner four hundred and sixty blocks from the
// other one, and a region built from that pair reads as its own complement — which is exactly what /fill, /fillbiome
// and /clone then refuse. Downstream nothing needs the fold either: Level.getChunk wraps the chunk it reaches for, an
// entity is filed in the physical section as it moves and normalised at the tail of its tick, and packet translation
// lays every position into the client's own frame. Where a coordinate is stored rather than consumed — a respawn
// position, the world border's centre — the wrap belongs at that sink, in front of whatever persists it.
//
// Only the horizontal axes: y has no bounds to leave, and vanilla already guards build height.
//
// getPosition does not declare CommandSyntaxException, but the handler throws one anyway — checked exceptions are a
// compiler rule, not a runtime one, and every caller of this method sits inside command execution, where brigadier
// catches it and prints it to the sender like any other command error.
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
