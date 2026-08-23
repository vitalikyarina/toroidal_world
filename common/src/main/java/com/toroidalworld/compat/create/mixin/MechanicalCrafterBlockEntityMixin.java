package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import com.toroidalworld.compat.create.CrafterGroupFold;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

@Mixin(value = MechanicalCrafterBlockEntity.class, remap = false)
public abstract class MechanicalCrafterBlockEntityMixin {
    @Unique
    private boolean toroidal$deltasNormalized;

    @Inject(method = "read", at = @At("RETURN"))
    private void toroidal$normalizeOnRead(CompoundTag compound, HolderLookup.Provider registries,
            boolean clientPacket, CallbackInfo ci) {
        toroidal$normalizeWithLevel();
    }

    // Neither hook covers the other: on chunk load read has no level yet, and a loaded reload never re-ticks first.
    @Inject(method = "tick", at = @At("HEAD"))
    private void toroidal$normalizeOnFirstTick(CallbackInfo ci) {
        toroidal$normalizeWithLevel();
    }

    @Unique
    private void toroidal$normalizeWithLevel() {
        if (toroidal$deltasNormalized) {
            return;
        }

        MechanicalCrafterBlockEntity crafter = (MechanicalCrafterBlockEntity) (Object) this;
        Level level = crafter.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }

        toroidal$deltasNormalized = true;
        CrafterGroupFold.normalizeOwn(level, crafter);
    }
}
