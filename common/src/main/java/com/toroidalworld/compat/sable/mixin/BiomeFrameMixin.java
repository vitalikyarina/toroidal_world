package com.toroidalworld.compat.sable.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.toroidalworld.compat.sable.SeamFrame;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;

@Mixin(Biome.class)
public abstract class BiomeFrameMixin {
    @WrapMethod(method = "shouldFreeze(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)Z")
    private boolean toroidal$frameOnFreeze(LevelReader reader, BlockPos pos, boolean mustBeAtEdge, Operation<Boolean> original) {
        Vec3 centre = Vec3.atCenterOf(pos);
        return SeamFrame.with(reader, () -> centre, () -> original.call(reader, pos, mustBeAtEdge));
    }
}
