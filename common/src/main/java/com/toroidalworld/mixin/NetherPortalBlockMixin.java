package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.core.DimensionMapping;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.portal.DimensionTransition;

@Mixin(NetherPortalBlock.class)
public class NetherPortalBlockMixin {
    @WrapOperation(
            method = "getPortalDestination",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/NetherPortalBlock;getExitPortal(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/level/border/WorldBorder;)Lnet/minecraft/world/level/portal/DimensionTransition;"))
    private DimensionTransition toroidal$wrappedExitPosition(
            NetherPortalBlock self,
            ServerLevel newLevel,
            Entity entity,
            BlockPos portalEntryPos,
            BlockPos approximateExitPos,
            boolean toNether,
            WorldBorder worldBorder,
            Operation<DimensionTransition> original) {
        WorldFold destination = WorldLoopAttachments.wrappedTransformerOf(newLevel);
        WorldFold source = WorldLoopAttachments.wrappedTransformerOf(entity.level());
        if (destination == null || source == null) {
            return original.call(self, newLevel, entity, portalEntryPos, approximateExitPos, toNether, worldBorder);
        }

        double declaredScale = DimensionType.getTeleportationScale(
                entity.level().dimensionType(), newLevel.dimensionType());
        BlockPos mapped = BlockPos.containing(DimensionMapping.map(source, destination, entity.position(), declaredScale));

        return original.call(self, newLevel, entity, portalEntryPos, mapped, toNether, worldBorder);
    }
}
