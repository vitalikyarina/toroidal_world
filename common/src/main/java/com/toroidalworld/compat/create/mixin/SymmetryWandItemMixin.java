package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.equipment.symmetryWand.SymmetryWandItem;
import com.toroidalworld.compat.create.CreateSeamFold;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

@Mixin(value = SymmetryWandItem.class, remap = false)
public class SymmetryWandItemMixin {
    @WrapOperation(method = "apply",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;distanceTo(Lnet/minecraft/world/phys/Vec3;)D"))
    private static double toroidal$foldApplyReach(Vec3 mirror, Vec3 placed, Operation<Double> original,
            Level world, ItemStack wand, Player player, BlockPos pos, BlockState block) {
        return original.call(CreateSeamFold.nearestCopy(world, placed, mirror), placed);
    }

    @WrapOperation(method = "remove",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;distanceTo(Lnet/minecraft/world/phys/Vec3;)D"))
    private static double toroidal$foldRemoveReach(Vec3 mirror, Vec3 broken, Operation<Double> original,
            Level world, ItemStack wand, Player player, BlockPos pos) {
        return original.call(CreateSeamFold.nearestCopy(world, broken, mirror), broken);
    }
}
