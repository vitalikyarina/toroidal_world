package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.deployer.DeployerMovementBehaviour;
import com.toroidalworld.compat.create.CreateSchematicFold;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@Mixin(value = DeployerMovementBehaviour.class, remap = false)
public class DeployerSchematicPrinterMixin {
    @ModifyVariable(method = "activateAsSchematicPrinter", argsOnly = true, at = @At("HEAD"))
    private BlockPos toroidal$visitInSchematicFrame(BlockPos visited, @Local(argsOnly = true) Level level,
            @Local(argsOnly = true) ItemStack filter) {
        return CreateSchematicFold.visitedInSchematicFrame(level, filter, visited);
    }
}
