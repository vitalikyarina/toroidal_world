package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamRange;
import com.toroidalworld.probe.ReseatProbe;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromBlockMemory;
import net.minecraft.world.entity.npc.Villager;

// The behaviour that walks a villager to the places it keeps — its bed, its job site, the meeting point. Each of them
// is a remembered position inside the world, so one lying ten blocks past the seam reads a whole world away, and every
// one of the behaviour's three readings is taken raw.
//
// The first decides the villager is too far to walk and sends it wandering instead; the wander step now points the
// right way, so the villager does eventually stumble across the seam and arrive — in fifteen-block hops, over the
// minute the memory is allowed to stay unreachable, instead of the straight walk it asked for. The third never lets it
// stop, because "close enough" cannot be met from the wrong side of the world. The one in the middle measures a
// candidate against the villager, both on the same side, and is folded here only because it shares the call.
//
// One wrap on the reading itself, not on the remembered position: the position also becomes the walk target, and a
// target written past the bounds would go stale by a whole world width the moment its owner crosses the seam.
@Mixin(SetWalkTargetFromBlockMemory.class)
public class SetWalkTargetFromBlockMemoryMixin {
    @WrapOperation(
            method = "*",
            require = 3,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;distManhattan(Lnet/minecraft/core/Vec3i;)I"))
    private static int toroidal$memoryDistanceThroughSeam(BlockPos from, Vec3i to, Operation<Integer> original,
            @Local(argsOnly = true) Villager body) {
        return ReseatProbe.decided(body.level(), ReseatProbe.MEMORY_DISTANCE, "blocks",
                original.call(from, to),
                SeamRange.manhattan(body, from, to));
    }
}
