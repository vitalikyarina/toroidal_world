package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import com.toroidalworld.accessors.SeamKeyedBlockEntity;
import com.toroidalworld.compat.create.CrafterGroupFold;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

@Mixin(value = MechanicalCrafterBlockEntity.class, remap = false)
public abstract class MechanicalCrafterBlockEntityMixin implements SeamKeyedBlockEntity {
    @Inject(method = "read", at = @At("RETURN"))
    private void toroidal$normalizeOnRead(CompoundTag compound, HolderLookup.Provider registries,
            boolean clientPacket, CallbackInfo ci) {
        toroidal$normalizeWithLevel();
    }

    @Override
    public void toroidal$rekey() {
        toroidal$normalizeWithLevel();
    }

    @Unique
    private void toroidal$normalizeWithLevel() {
        MechanicalCrafterBlockEntity crafter = (MechanicalCrafterBlockEntity) (Object) this;
        if (!(crafter.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        CrafterGroupFold.normalizeOwn(serverLevel, crafter);
    }
}
