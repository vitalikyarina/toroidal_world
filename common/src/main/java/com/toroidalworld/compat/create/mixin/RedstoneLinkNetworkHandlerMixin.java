package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler;
import com.toroidalworld.compat.create.CreateSeamFold;
import com.toroidalworld.compat.create.FoldedLinkable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

@Mixin(value = RedstoneLinkNetworkHandler.class, remap = false)
public class RedstoneLinkNetworkHandlerMixin {
    @WrapOperation(method = "updateNetworkOf",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/redstone/link/RedstoneLinkNetworkHandler;withinRange("
                            + "Lcom/simibubi/create/content/redstone/link/IRedstoneLinkable;"
                            + "Lcom/simibubi/create/content/redstone/link/IRedstoneLinkable;)Z"))
    private boolean toroidal$foldLinkRange(IRedstoneLinkable from, IRedstoneLinkable to,
            Operation<Boolean> original, LevelAccessor world) {
        if (from == to || !(world instanceof Level level)) {
            return original.call(from, to);
        }

        BlockPos storedFrom = from.getLocation();
        BlockPos storedTo = to.getLocation();
        BlockPos seatedFrom = CreateSeamFold.worldSeat(level, storedFrom);
        BlockPos seatedTo = CreateSeamFold.worldSeat(level, storedTo);
        BlockPos foldedTo = CreateSeamFold.nearestCopy(level, seatedFrom, seatedTo);
        if (seatedFrom.equals(storedFrom) && foldedTo.equals(storedTo)) {
            return original.call(from, to);
        }

        return original.call(new FoldedLinkable(from, seatedFrom), new FoldedLinkable(to, foldedTo));
    }
}
