package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.simibubi.create.content.kinetics.gantry.GantryShaftBlock;
import com.toroidalworld.compat.create.CreateWalkClosure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(value = GantryShaftBlock.class, remap = false)
public abstract class GantryShaftBlockMixin {
    @WrapOperation(method = "neighborChanged", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
            ordinal = 0))
    private BlockState toroidal$closeTheRing(Level world, BlockPos pos, Operation<BlockState> original,
            @Share("walkClosure") LocalRef<CreateWalkClosure> closureRef,
            @Share("walkClosureResolved") LocalBooleanRef resolved) {
        if (!resolved.get()) {
            closureRef.set(CreateWalkClosure.of(world));
            resolved.set(true);
        }

        CreateWalkClosure closure = closureRef.get();
        if (closure == null || !closure.closes(pos)) {
            return original.call(world, pos);
        }

        return Blocks.AIR.defaultBlockState();
    }
}
