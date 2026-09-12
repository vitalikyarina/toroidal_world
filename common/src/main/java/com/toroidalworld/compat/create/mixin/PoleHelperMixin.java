package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.simibubi.create.foundation.placement.PoleHelper;
import com.toroidalworld.InjectionTargets;
import com.toroidalworld.compat.create.CreateWalkClosure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(value = PoleHelper.class, remap = false)
public abstract class PoleHelperMixin {
    @WrapOperation(method = "attachedPoles", at = @At(value = "INVOKE",
            target = InjectionTargets.LEVEL_GET_BLOCK_STATE))
    private BlockState toroidal$closeTheRing(Level world, BlockPos pos, Operation<BlockState> original,
            @Share("walkClosure") LocalRef<CreateWalkClosure> closureRef,
            @Share("walkClosureResolved") LocalBooleanRef resolved) {
        return CreateWalkClosure.read(world, pos, original, closureRef, resolved);
    }
}
