package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.phys.Vec3;

@Mixin(TheEndGatewayBlockEntity.class)
public class TheEndGatewayBlockEntityMixin {
    @WrapMethod(method = "findExitPortalXZPosTentative(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;")
    private static Vec3 toroidal$foldTentativeTarget(ServerLevel level, BlockPos endGatewayPos, Operation<Vec3> original) {
        return WorldLoopAttachments.transformerOf(level).fold(original.call(level, endGatewayPos));
    }

    @WrapMethod(method = "setExitPosition(Lnet/minecraft/core/BlockPos;Z)V")
    private void toroidal$foldExitPosition(BlockPos exactPosition, boolean exact, Operation<Void> original) {
        WorldFold transformer =
                WorldLoopAttachments.wrappedTransformerOf(((BlockEntity) (Object) this).getLevel());
        original.call(transformer == null ? exactPosition : transformer.fold(exactPosition), exact);
    }
}
