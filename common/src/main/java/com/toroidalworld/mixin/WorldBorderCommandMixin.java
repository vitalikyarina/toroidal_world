package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.WorldBorderCommand;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

@Mixin(WorldBorderCommand.class)
public class WorldBorderCommandMixin {
    @ModifyVariable(method = "setCenter", at = @At("HEAD"), argsOnly = true)
    private static Vec2 toroidal$storeCentreInsideBounds(Vec2 center,
            @Local(argsOnly = true) CommandSourceStack source) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(source.getLevel());
        if (transformer == null) {
            return center;
        }

        Vec3 folded = transformer.fold(new Vec3(center.x, 0.0, center.y));
        return new Vec2((float) folded.x, (float) folded.z);
    }
}
