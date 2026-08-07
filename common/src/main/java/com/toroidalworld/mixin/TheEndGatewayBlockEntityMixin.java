package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.phys.Vec3;

// Gateway teleports are computed and stored in raw origin-anchored coordinates: the tentative target sits 1024 blocks
// out plus up to a 16-chunk (256-block) walk, and the result is persisted as an absolute exit_portal BlockPos. On the
// current floor of 192 chunks (3072 blocks) the whole reach stays inside the half-width, so both folds below are the
// identity — they are here because the primitive gets wrapped rather than trusted by distance: a smaller End (a future
// floor, a datapack) would otherwise store and teleport to positions past the seam silently.
@Mixin(TheEndGatewayBlockEntity.class)
public class TheEndGatewayBlockEntityMixin {
    @WrapMethod(method = "findExitPortalXZPosTentative(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;")
    private static Vec3 toroidal$foldTentativeTarget(ServerLevel level, BlockPos endGatewayPos, Operation<Vec3> original) {
        Vec3 tentative = original.call(level, endGatewayPos);
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        return transformer == null ? tentative : transformer.vectors.wrap(tentative);
    }

    // The single write funnel for exit_portal: the fresh computation above and a datapack's exact-teleport gateway both
    // land here, so a stored exit position can never name a place past the bounds.
    @WrapMethod(method = "setExitPosition(Lnet/minecraft/core/BlockPos;Z)V")
    private void toroidal$foldExitPosition(BlockPos exactPosition, boolean exact, Operation<Void> original) {
        Level selfLevel = ((BlockEntity) (Object) this).getLevel();
        WorldLoopTransformer transformer = selfLevel == null ? null : WorldLoopAttachments.wrappedTransformerOf(selfLevel);
        original.call(transformer == null ? exactPosition : transformer.blocks.wrap(exactPosition), exact);
    }
}
