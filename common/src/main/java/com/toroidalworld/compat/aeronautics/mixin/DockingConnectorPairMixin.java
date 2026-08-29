package com.toroidalworld.compat.aeronautics.mixin;

import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.toroidalworld.compat.aeronautics.MagnetSeamFrame;

import dev.simulated_team.simulated.content.blocks.docking_connector.DockingConnectorBlockEntity;
import dev.simulated_team.simulated.content.blocks.docking_connector.DockingConnectorPair;

@Mixin(value = DockingConnectorPair.class, remap = false)
public class DockingConnectorPairMixin {
    @ModifyReturnValue(method = "getAverageTipPosition", at = @At("RETURN"))
    private static Vector3d toroidal$foldTipMidpoint(Vector3d average, DockingConnectorBlockEntity dock1,
            DockingConnectorBlockEntity dock2, Vector3d dest) {
        return MagnetSeamFrame.midpoint(dock1, dock2, average);
    }
}
