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
