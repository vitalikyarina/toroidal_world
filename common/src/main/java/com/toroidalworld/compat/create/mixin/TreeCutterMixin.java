package com.toroidalworld.compat.create.mixin;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.simibubi.create.content.kinetics.saw.TreeCutter;
import com.toroidalworld.compat.create.CanonicalPositionKeys;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(value = TreeCutter.class, remap = false)
public class TreeCutterMixin {
    @ModifyVariable(method = "validateCut", at = @At("STORE"), ordinal = 0)
    private static Set<BlockPos> toroidal$canonicalGateVisited(Set<BlockPos> visited, BlockGetter reader,
            BlockPos pos) {
        return CanonicalPositionKeys.set(reader);
    }

    @ModifyVariable(method = "findTree", at = @At("STORE"), ordinal = 0)
    private static Set<BlockPos> toroidal$canonicalTreeVisited(Set<BlockPos> visited, BlockGetter reader, BlockPos pos,
            BlockState brokenState) {
        return CanonicalPositionKeys.set(reader);
    }
}
