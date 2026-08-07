package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.portal.TeleportTransition;

// Where a portal comes out. Vanilla builds that position from DimensionType.coordinateScale — the nether's hardcoded
// 8 — and then clamps the result to the world border. Both halves are wrong here.
//
// The scale is wrong because a looped world chooses its own: the nether wraps at overworldWidth / scale, and that ratio
// is the portal mapping. It is read back off the two dimensions rather than stored anywhere, since each already carries
// its own bounds — the transformer answers it per axis, and an axis that does not close in both worlds has no width for
// a ratio to be read from, so there the scale the dimensions declare stands, exactly as vanilla applied it.
//
// The clamp is wrong independently of the scale. Past the bounds there is no edge to be pushed against — the ground
// continues on the other side — so a player stepping through near the seam was dropped at the edge of the world instead
// of at the place their portal actually maps to.
@Mixin(NetherPortalBlock.class)
public class NetherPortalBlockMixin {
    @WrapOperation(
            method = "getPortalDestination",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/NetherPortalBlock;getExitPortal(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/level/border/WorldBorder;)Lnet/minecraft/world/level/portal/TeleportTransition;"))
    private TeleportTransition toroidal$wrappedExitPosition(
            NetherPortalBlock self,
            ServerLevel newLevel,
            Entity entity,
            BlockPos portalEntryPos,
            BlockPos approximateExitPos,
            boolean toNether,
            WorldBorder worldBorder,
            Operation<TeleportTransition> original) {
        WorldLoopTransformer destination = WorldLoopAttachments.wrappedTransformerOf(newLevel);
        WorldLoopTransformer source = WorldLoopAttachments.wrappedTransformerOf(entity.level());
        if (destination == null || source == null) {
            return original.call(self, newLevel, entity, portalEntryPos, approximateExitPos, toNether, worldBorder);
        }

        double declaredScale = DimensionType.getTeleportationScale(
                entity.level().dimensionType(), newLevel.dimensionType());
        BlockPos mapped = BlockPos.containing(destination.mapFrom(source, entity.position(), declaredScale));

        return original.call(self, newLevel, entity, portalEntryPos, mapped, toNether, worldBorder);
    }
}
