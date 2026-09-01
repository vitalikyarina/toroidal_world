package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.phys.Vec3;

@Mixin(TheEndGatewayBlockEntity.class)
public class TheEndGatewayBlockEntityMixin {
    @WrapMethod(method = "findExitPortalXZPosTentative(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;")
    private static Vec3 toroidal$foldTentativeTarget(ServerLevel level, BlockPos endGatewayPos, Operation<Vec3> original) {
        Vec3 tentative = original.call(level, endGatewayPos);
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        return transformer == null ? tentative : transformer.fold(tentative);
    }

    @WrapMethod(method = "setExitPosition(Lnet/minecraft/core/BlockPos;Z)V")
    private void toroidal$foldExitPosition(BlockPos exactPosition, boolean exact, Operation<Void> original) {
        Level selfLevel = ((BlockEntity) (Object) this).getLevel();
        WorldFold transformer = selfLevel == null ? null : WorldLoopAttachments.wrappedTransformerOf(selfLevel);
        original.call(transformer == null ? exactPosition : transformer.fold(exactPosition), exact);
    }
}
