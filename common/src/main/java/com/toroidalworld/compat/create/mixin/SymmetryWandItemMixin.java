package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.equipment.symmetryWand.SymmetryWandItem;
import com.simibubi.create.content.equipment.symmetryWand.mirror.SymmetryMirror;
import com.toroidalworld.compat.create.SymmetryMirrorFold;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

@Mixin(value = SymmetryWandItem.class, remap = false)
public class SymmetryWandItemMixin {
    @ModifyVariable(method = "apply", at = @At("STORE"), ordinal = 0)
    private static SymmetryMirror toroidal$foldApplyMirror(SymmetryMirror mirror,
            @Local(argsOnly = true) Level world, @Local(argsOnly = true) BlockPos pos) {
        return SymmetryMirrorFold.nearestTo(world, mirror, pos);
    }

    @ModifyVariable(method = "remove", at = @At("STORE"), ordinal = 0)
    private static SymmetryMirror toroidal$foldRemoveMirror(SymmetryMirror mirror,
            @Local(argsOnly = true) Level world, @Local(argsOnly = true) BlockPos pos) {
        return SymmetryMirrorFold.nearestTo(world, mirror, pos);
    }
}
