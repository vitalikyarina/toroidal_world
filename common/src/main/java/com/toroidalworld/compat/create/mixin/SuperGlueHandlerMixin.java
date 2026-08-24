package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.contraptions.glue.SuperGlueHandler;
import com.toroidalworld.compat.create.CreateSeamFold;

import java.util.function.BiFunction;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(value = SuperGlueHandler.class, remap = false)
public class SuperGlueHandlerMixin {
    @ModifyVariable(method = "glueInOffHandAppliesOnBlockPlace", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static BlockPos toroidal$canonicalizeThePlacedBlock(BlockPos placed,
            @Local(argsOnly = true) Player placer) {
        return placer.level() instanceof ServerLevel serverLevel
                ? CreateSeamFold.canonical(serverLevel, placed)
                : placed;
    }

    @ModifyArg(method = "glueInOffHandAppliesOnBlockPlace",
            at = @At(value = "INVOKE",
                    target = "Lnet/createmod/catnip/levelWrappers/RayTraceLevel;<init>(Lnet/minecraft/world/level/LevelAccessor;Ljava/util/function/BiFunction;)V"),
            index = 1)
    private static BiFunction<BlockPos, BlockState, BlockState> toroidal$traceAgainstTheCanonicalBlock(
            BiFunction<BlockPos, BlockState, BlockState> stateGetter, @Local(argsOnly = true) Player placer) {
        if (!(placer.level() instanceof ServerLevel serverLevel)) {
            return stateGetter;
        }

        return (tracePos, state) -> stateGetter.apply(CreateSeamFold.canonical(serverLevel, tracePos), state);
    }

    @WrapOperation(method = "glueInOffHandAppliesOnBlockPlace",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;equals(Ljava/lang/Object;)Z"))
    private static boolean toroidal$namesThePlacedBlock(BlockPos hitNeighbour, Object placed,
            Operation<Boolean> original, @Local(argsOnly = true) Player placer) {
        BlockPos namedNeighbour = placer.level() instanceof ServerLevel serverLevel
                ? CreateSeamFold.canonical(serverLevel, hitNeighbour)
                : hitNeighbour;
        return original.call(namedNeighbour, placed);
    }
}
