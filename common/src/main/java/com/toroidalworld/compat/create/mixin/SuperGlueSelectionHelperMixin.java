package com.toroidalworld.compat.create.mixin;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.simibubi.create.content.contraptions.glue.SuperGlueSelectionHelper;
import com.toroidalworld.compat.create.CanonicalPositionKeys;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

@Mixin(value = SuperGlueSelectionHelper.class, remap = false)
public class SuperGlueSelectionHelperMixin {
    @ModifyVariable(method = "searchGlueGroup", at = @At("STORE"), ordinal = 0)
    private static Set<BlockPos> toroidal$canonicalVisited(Set<BlockPos> visited, Level level, BlockPos startPos,
            BlockPos endPos, boolean includeOther) {
        return CanonicalPositionKeys.set(level);
    }
}
