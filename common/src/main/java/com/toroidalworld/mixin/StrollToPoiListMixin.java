package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamRange;
import com.toroidalworld.probe.ReseatProbe;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.ai.behavior.StrollToPoiList;
import net.minecraft.world.entity.npc.Villager;

// Two remembered places at once: one is picked to walk to, a second has to still be close by for the walk to be allowed
// at all. Only the second is measured, and it is the anchor — the villager's meeting point, kept in the world while the
// villager itself is wrapped. Across the seam the anchor never holds, so the whole list of secondary sites goes unused.
@Mixin(StrollToPoiList.class)
public class StrollToPoiListMixin {
    @WrapOperation(
            method = "*",
            require = 1,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;closerToCenterThan(Lnet/minecraft/core/Position;D)Z"))
    private static boolean toroidal$anchorTetherThroughSeam(BlockPos anchorPos, Position bodyPosition, double distance,
            Operation<Boolean> original, @Local(argsOnly = true) Villager body) {
        return ReseatProbe.decided(body.level(), ReseatProbe.ANCHOR_TETHER,
                original.call(anchorPos, bodyPosition, distance),
                SeamRange.closerToCenterThan(body, anchorPos, bodyPosition, distance));
    }
}
