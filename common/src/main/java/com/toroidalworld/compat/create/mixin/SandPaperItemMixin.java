package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.equipment.sandPaper.SandPaperItem;
import com.toroidalworld.VanillaInvokeTargets;
import com.toroidalworld.compat.create.CreateSeamFold;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(value = SandPaperItem.class, remap = false)
public class SandPaperItemMixin {
    @WrapOperation(method = "use",
            at = @At(value = "INVOKE",
                    target = VanillaInvokeTargets.VEC3_DISTANCE_TO))
    private double toroidal$foldPickUpReach(Vec3 item, Vec3 player, Operation<Double> original,
            Level worldIn, Player playerIn, InteractionHand handIn) {
        return original.call(CreateSeamFold.foldPoint(worldIn, player, item), player);
    }
}
