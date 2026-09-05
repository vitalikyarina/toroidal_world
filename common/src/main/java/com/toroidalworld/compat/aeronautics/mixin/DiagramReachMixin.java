package com.toroidalworld.compat.aeronautics.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.core.FoldedBoxQuery;
import com.toroidalworld.storage.WorldLoopAttachments;

import dev.simulated_team.simulated.content.entities.diagram.DiagramEntity;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

@Mixin(value = DiagramEntity.class, remap = false)
public class DiagramReachMixin {
    @ModifyVariable(method = "canPlayerUse", at = @At("STORE"), ordinal = 0)
    private AABB toroidal$foldDiagramBox(AABB box, Player player) {
        return FoldedBoxQuery.toward(
                WorldLoopAttachments.wrappedTransformerOfReader(player.level()), player.position(), box);
    }
}
