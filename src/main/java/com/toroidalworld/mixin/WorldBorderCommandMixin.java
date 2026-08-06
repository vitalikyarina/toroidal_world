package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.WorldBorderCommand;
import net.minecraft.world.phys.Vec2;

// The third coordinate the server writes down instead of spending, after the two respawn points. It ends up in
// level.dat and comes back on every load, so a centre named from past the bounds would outlive the command that named
// it — and the border would be measured around ground the world does not have.
//
// Settled on the way into the command rather than at WorldBorder.setCenter, which is where the rule would rather live:
// a border belongs to a level but does not know it, and it is handed two bare doubles with nothing to ask which world
// they are in. The command is one step out and still holds the sender's level. Unlike the respawn commands there is no
// second way in — a centre can only be named as an argument — so one step out is enough.
//
// At the head, so the whole method reads the settled centre: the "already centred there" refusal compares against it,
// and the message printed back names a coordinate this world has.
@Mixin(WorldBorderCommand.class)
public class WorldBorderCommandMixin {
    @ModifyVariable(method = "setCenter", at = @At("HEAD"), argsOnly = true)
    private static Vec2 toroidal$storeCentreInsideBounds(Vec2 center,
            @Local(argsOnly = true) CommandSourceStack source) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(source.getLevel());
        if (transformer == null) {
            return center;
        }

        return new Vec2(
                (float) transformer.coords.x.wrap(center.x),
                (float) transformer.coords.z.wrap(center.y));
    }
}
