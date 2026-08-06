package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.world.entity.projectile.FishingHook;

// Reeling in is a shove toward the angler, worked out as the plain difference between where they stand and where the
// bobber floats. Across the seam that difference is a world wide and points the wrong way, and nothing downstream
// tempers it: the catch is handed a tenth of it as velocity outright, which is some four hundred blocks in a single
// tick, away from the rod. The loot throw adds its own twist — the lift it derives from the same length is the fourth
// root of it, so a world-wide gap lofts the catch some twenty-eight times higher than a real one would.
//
// Both are settled by reading the angler at the copy nearest the bobber. The hook's own coordinates fold to themselves,
// so a rod cast on this side is untouched in every number.
//
// The loot block reads the angler twice and only the first reading is a difference; the second places the experience
// orb, which is a position in the world and must stay the one the angler really occupies.
@Mixin(FishingHook.class)
public class FishingHookMixin {
    @ModifyExpressionValue(
            method = "pullEntity",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getX()D"))
    private double toroidal$pullTowardsOwnerX(double ownerX) {
        return SeamAim.nearX((FishingHook) (Object) this, ownerX);
    }

    @ModifyExpressionValue(
            method = "pullEntity",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getZ()D"))
    private double toroidal$pullTowardsOwnerZ(double ownerZ) {
        return SeamAim.nearZ((FishingHook) (Object) this, ownerZ);
    }

    @ModifyExpressionValue(
            method = "retrieve",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getX()D", ordinal = 0))
    private double toroidal$throwLootTowardsOwnerX(double ownerX) {
        return SeamAim.nearX((FishingHook) (Object) this, ownerX);
    }

    @ModifyExpressionValue(
            method = "retrieve",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getZ()D", ordinal = 0))
    private double toroidal$throwLootTowardsOwnerZ(double ownerZ) {
        return SeamAim.nearZ((FishingHook) (Object) this, ownerZ);
    }
}
