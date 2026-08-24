package com.toroidalworld.compat.create.mixin;

import java.util.HashSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.simibubi.create.content.logistics.tunnel.BrassTunnelBlockEntity;
import com.toroidalworld.storage.WorldLoopAttachments;

@Mixin(value = BrassTunnelBlockEntity.class, remap = false)
public abstract class BrassTunnelBlockEntityMixin {
    @WrapOperation(method = { "grabAllStacksOfGroup", "gatherValidOutputs" },
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/logistics/tunnel/BrassTunnelBlockEntity;getAdjacent(Z)Lcom/simibubi/create/content/logistics/tunnel/BrassTunnelBlockEntity;"))
    private @Nullable BrassTunnelBlockEntity toroidal$closeTheGroupChain(BrassTunnelBlockEntity walker,
            boolean leftSide, Operation<BrassTunnelBlockEntity> original,
            @Share("visitedTunnels") LocalRef<Set<BrassTunnelBlockEntity>> visitedRef,
            @Share("visitedTunnelsResolved") LocalBooleanRef resolved) {
        BrassTunnelBlockEntity adjacent = original.call(walker, leftSide);
        if (adjacent == null) {
            return null;
        }

        if (!resolved.get()) {
            if (WorldLoopAttachments.wrappedTransformerOfReader(walker.getLevel()) != null) {
                Set<BrassTunnelBlockEntity> visited = new HashSet<>();
                visited.add(walker);
                visitedRef.set(visited);
            }

            resolved.set(true);
        }

        Set<BrassTunnelBlockEntity> visited = visitedRef.get();
        if (visited == null) {
            return adjacent;
        }

        return visited.add(adjacent) ? adjacent : null;
    }
}
