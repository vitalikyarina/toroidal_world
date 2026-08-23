package com.toroidalworld.compat.create.mixin;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.crafter.ConnectedInputHandler;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import com.toroidalworld.compat.create.CrafterGroupFold;
import com.toroidalworld.compat.create.CreateSeamFold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;

@Mixin(value = ConnectedInputHandler.class, remap = false)
public abstract class ConnectedInputHandlerMixin {
    @WrapMethod(method = "toggleConnection")
    private static void toroidal$toggleInOneFrame(Level world, BlockPos pos, BlockPos pos2,
            Operation<Void> original) {
        original.call(world, CreateSeamFold.canonical(world, pos), CreateSeamFold.canonical(world, pos2));
    }

    @WrapOperation(method = "toggleConnection",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;offset(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/core/BlockPos;"))
    private static BlockPos toroidal$controllerInCanonicalFrame(BlockPos position, Vec3i delta,
            Operation<BlockPos> original, @Local(argsOnly = true) Level world) {
        return CreateSeamFold.canonical(world, original.call(position, delta));
    }

    @SuppressWarnings("unchecked")
    @WrapOperation(method = "toggleConnection",
            at = @At(value = "INVOKE",
                    target = "Ljava/util/stream/Stream;collect(Ljava/util/stream/Collector;)Ljava/lang/Object;"))
    private static Object toroidal$membersInCanonicalFrame(Stream<?> stream, Collector<?, ?, ?> collector,
            Operation<Object> original, @Local(argsOnly = true) Level world) {
        Object collected = original.call(stream, collector);
        if (!(collected instanceof Set<?> members)) {
            return collected;
        }

        return CrafterGroupFold.canonicalMembers(world, (Set<BlockPos>) members);
    }

    @WrapOperation(method = "toggleConnection",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;relative(Lnet/minecraft/core/Direction;)Lnet/minecraft/core/BlockPos;"))
    private static BlockPos toroidal$floodStepInCanonicalFrame(BlockPos current, Direction direction,
            Operation<BlockPos> original, @Local(argsOnly = true) Level world) {
        return CreateSeamFold.canonical(world, original.call(current, direction));
    }

    @Inject(method = "initAndAddAll", at = @At("RETURN"))
    private static void toroidal$normalizeAfterInitAndAddAll(Level world, MechanicalCrafterBlockEntity crafter,
            Collection<BlockPos> positions, CallbackInfo ci) {
        CrafterGroupFold.normalizeGroup(world, crafter);
    }

    @Inject(method = "connectControllers", at = @At("RETURN"))
    private static void toroidal$normalizeAfterConnectControllers(Level world, MechanicalCrafterBlockEntity crafter1,
            MechanicalCrafterBlockEntity crafter2, CallbackInfo ci) {
        CrafterGroupFold.normalizeGroup(world, crafter1);
    }
}
