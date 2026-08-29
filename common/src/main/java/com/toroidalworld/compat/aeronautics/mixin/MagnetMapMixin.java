package com.toroidalworld.compat.aeronautics.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.toroidalworld.compat.aeronautics.MagnetSectionKeys;

import dev.simulated_team.simulated.content.blocks.redstone_magnet.MagnetMap;
import dev.simulated_team.simulated.util.SimMovementContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LevelAccessor;

@Mixin(value = MagnetMap.class, remap = false)
public class MagnetMapMixin {
    @WrapMethod(method = "addMagnet")
    private void toroidal$addAtPhysicalSection(LevelAccessor level, SectionPos sectionPos, BlockPos pos,
            Operation<Void> original) {
        original.call(level, MagnetSectionKeys.physical(level, sectionPos), pos);
    }

    @WrapMethod(method = "removeMagnet")
    private void toroidal$removeAtPhysicalSection(LevelAccessor level, SectionPos sectionPos, BlockPos pos,
            Operation<Void> original) {
        original.call(level, MagnetSectionKeys.physical(level, sectionPos), pos);
    }

    @WrapOperation(
            method = "findNearby",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object toroidal$searchPhysicalSection(Map<?, ?> sections, Object key, Operation<Object> original,
            SimMovementContext context) {
        return MagnetSectionKeys.lookup(sections, key, context.level(), original);
    }
}
