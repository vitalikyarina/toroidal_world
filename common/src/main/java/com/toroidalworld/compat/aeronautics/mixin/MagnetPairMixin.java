package com.toroidalworld.compat.aeronautics.mixin;

import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.toroidalworld.compat.aeronautics.MagnetSeamDelta;

import dev.simulated_team.simulated.content.blocks.redstone_magnet.MagnetPair;
import dev.simulated_team.simulated.content.blocks.redstone_magnet.SimMagnet;

import net.minecraft.world.phys.Vec3;

@Mixin(value = MagnetPair.class, remap = false)
public class MagnetPairMixin {
    @ModifyReturnValue(
            method = "getRelativePosition(Ldev/simulated_team/simulated/content/blocks/redstone_magnet/SimMagnet;"
                    + "Ldev/simulated_team/simulated/content/blocks/redstone_magnet/SimMagnet;"
                    + "Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lorg/joml/Vector3d;)Lorg/joml/Vector3d;",
            at = @At("RETURN"))
    private static Vector3d toroidal$foldPairDelta(Vector3d relative, SimMagnet magnet1, SimMagnet magnet2,
            Vec3 plotPos1, Vec3 plotPos2, Vector3d dest) {
        return MagnetSeamDelta.fold(magnet1, relative);
    }
}
