package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.simibubi.create.content.equipment.blueprint.BlueprintEntity;
import com.toroidalworld.core.FoldedBoxQuery;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

@Mixin(value = BlueprintEntity.class, remap = false)
public class BlueprintReachMixin {
    @ModifyVariable(method = "canPlayerUse", at = @At("STORE"), ordinal = 0)
    private AABB toroidal$foldBlueprintBox(AABB box, Player player) {
        return FoldedBoxQuery.toward(
                WorldLoopAttachments.wrappedTransformerOfReader(player.level()), player.position(), box);
    }
}
