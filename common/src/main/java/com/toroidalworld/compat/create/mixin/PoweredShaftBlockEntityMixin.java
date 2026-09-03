package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.steamEngine.PoweredShaftBlockEntity;
import com.toroidalworld.accessors.SeamKeyedBlockEntity;
import com.toroidalworld.compat.create.CreateInvokeTargets;
import com.toroidalworld.compat.create.RelativeKeyFold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

@Mixin(value = PoweredShaftBlockEntity.class, remap = false)
public abstract class PoweredShaftBlockEntityMixin implements SeamKeyedBlockEntity {
    @WrapOperation(method = "update", at = @At(value = "INVOKE", target = CreateInvokeTargets.BLOCK_POS_SUBTRACT))
    private BlockPos toroidal$foldStoredKey(BlockPos worldPosition, Vec3i sourcePos, Operation<BlockPos> original) {
        return toroidal$fold(worldPosition, sourcePos, original.call(worldPosition, sourcePos));
    }

    @WrapOperation(method = "isPoweredBy", at = @At(value = "INVOKE", target = CreateInvokeTargets.BLOCK_POS_SUBTRACT))
    private BlockPos toroidal$foldComparedKey(BlockPos worldPosition, Vec3i globalPos, Operation<BlockPos> original) {
        return toroidal$fold(worldPosition, globalPos, original.call(worldPosition, globalPos));
    }

    @Inject(method = "read", at = @At("RETURN"))
    private void toroidal$normalizeOnRead(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket,
            CallbackInfo ci) {
        toroidal$normalizeWithLevel();
    }

    @Override
    public void toroidal$rekey() {
        toroidal$normalizeWithLevel();
    }

    @Unique
    private BlockPos toroidal$fold(BlockPos owner, Vec3i partner, BlockPos rawKey) {
        PoweredShaftBlockEntity self = (PoweredShaftBlockEntity) (Object) this;
        return RelativeKeyFold.shortWay(self.getLevel(), owner, partner, rawKey);
    }

    @Unique
    private void toroidal$normalizeWithLevel() {
        PoweredShaftBlockEntity self = (PoweredShaftBlockEntity) (Object) this;
        BlockPos storedKey = self.enginePos;
        if (storedKey == null || !(self.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos normalizedKey = RelativeKeyFold.normalize(serverLevel, self.getBlockPos(), storedKey);
        if (!normalizedKey.equals(storedKey)) {
            self.enginePos = normalizedKey;
        }
    }
}
