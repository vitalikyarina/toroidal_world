package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.steamEngine.PoweredShaftBlockEntity;
import com.toroidalworld.compat.create.RelativeKeyFold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

@Mixin(value = PoweredShaftBlockEntity.class, remap = false)
public abstract class PoweredShaftBlockEntityMixin {
    @Unique
    private static final String SUBTRACT =
            "Lnet/minecraft/core/BlockPos;subtract(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/core/BlockPos;";

    @Unique
    private boolean toroidal$normalizedOnTick;

    @WrapOperation(method = "update", at = @At(value = "INVOKE", target = SUBTRACT))
    private BlockPos toroidal$foldStoredKey(BlockPos worldPosition, Vec3i sourcePos, Operation<BlockPos> original) {
        return toroidal$fold(worldPosition, sourcePos, original.call(worldPosition, sourcePos));
    }

    @WrapOperation(method = "isPoweredBy", at = @At(value = "INVOKE", target = SUBTRACT))
    private BlockPos toroidal$foldComparedKey(BlockPos worldPosition, Vec3i globalPos, Operation<BlockPos> original) {
        return toroidal$fold(worldPosition, globalPos, original.call(worldPosition, globalPos));
    }

    @Inject(method = "read", at = @At("RETURN"))
    private void toroidal$normalizeOnRead(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket,
            CallbackInfo ci) {
        toroidal$normalizeWithLevel();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void toroidal$normalizeOnFirstTick(CallbackInfo ci) {
        if (toroidal$normalizedOnTick) {
            return;
        }

        PoweredShaftBlockEntity self = (PoweredShaftBlockEntity) (Object) this;
        if (self.getLevel() != null) {
            toroidal$normalizedOnTick = true;
            toroidal$normalizeWithLevel();
        }
    }

    @Unique
    private BlockPos toroidal$fold(BlockPos owner, Vec3i partner, BlockPos rawKey) {
        PoweredShaftBlockEntity self = (PoweredShaftBlockEntity) (Object) this;
        return RelativeKeyFold.shortWay(self.getLevel(), owner, partner, rawKey);
    }

    @Unique
    private void toroidal$normalizeWithLevel() {
        PoweredShaftBlockEntity self = (PoweredShaftBlockEntity) (Object) this;
        Level level = self.getLevel();
        BlockPos storedKey = self.enginePos;
        if (level == null || storedKey == null) {
            return;
        }

        BlockPos normalizedKey = RelativeKeyFold.normalize(level, self.getBlockPos(), storedKey);
        if (!normalizedKey.equals(storedKey)) {
            self.enginePos = normalizedKey;
        }
    }
}
