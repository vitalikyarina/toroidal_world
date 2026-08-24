package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.logistics.redstoneRequester.AutoRequestData;
import com.toroidalworld.compat.create.CreateSeamFold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@Mixin(value = AutoRequestData.class, remap = false)
public class AutoRequestDataMixin {
    @WrapOperation(method = "readFromItem",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;subtract(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/core/BlockPos;"))
    private static BlockPos toroidal$foldKeeperDelta(BlockPos target, Vec3i placed, Operation<BlockPos> original,
            Level level, Player player, BlockPos position, ItemStack itemStack) {
        BlockPos raw = original.call(target, placed);
        if (!(placed instanceof BlockPos anchor)) {
            return raw;
        }

        return CreateSeamFold.foldDelta(level, anchor, target, raw);
    }
}
