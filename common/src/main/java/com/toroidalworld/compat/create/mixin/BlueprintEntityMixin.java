package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.simibubi.create.content.equipment.blueprint.BlueprintEntity;
import com.toroidalworld.compat.create.CreateSeamFold;
import com.toroidalworld.compat.create.CreateTrackFold;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Mixin(value = BlueprintEntity.class, remap = false)
public class BlueprintEntityMixin {
    @ModifyVariable(method = "canPlayerUse", at = @At("STORE"), ordinal = 0)
    private AABB toroidal$foldBlueprintBox(AABB box, Player player) {
        return CreateTrackFold.foldBoxToward(player.level(), player.position(), box);
    }

    @ModifyVariable(method = "skipAttackInteraction", at = @At("STORE"), ordinal = 0)
    private Vec3 toroidal$foldAttackRay(Vec3 eyePos, Entity source) {
        Entity blueprint = (Entity) (Object) this;
        return CreateSeamFold.foldPoint(blueprint.level(), blueprint.position(), eyePos);
    }
}
