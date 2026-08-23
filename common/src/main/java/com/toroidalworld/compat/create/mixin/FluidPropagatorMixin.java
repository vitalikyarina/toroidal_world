package com.toroidalworld.compat.create.mixin;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.simibubi.create.content.fluids.FluidPropagator;
import com.toroidalworld.compat.create.CanonicalPositionKeys;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

@Mixin(value = FluidPropagator.class, remap = false)
public class FluidPropagatorMixin {
    @ModifyVariable(method = "resetAffectedFluidNetworks", at = @At("STORE"), ordinal = 0)
    private static Set<BlockPos> toroidal$canonicalVisited(Set<BlockPos> visited, Level world, BlockPos start,
            Direction side) {
        return CanonicalPositionKeys.set(world);
    }
}
