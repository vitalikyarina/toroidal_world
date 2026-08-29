package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.toroidalworld.compat.create.SyncedTagFold;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

@Mixin(value = SmartBlockEntity.class, remap = false)
public class SmartBlockEntitySyncedTagMixin {
    @ModifyVariable(method = { "loadAdditional", "readClient" }, at = @At("HEAD"), argsOnly = true)
    private CompoundTag toroidal$foldSyncedPositionsIntoOwnFrame(CompoundTag tag) {
        return SyncedTagFold.inFrameOf((BlockEntity) (Object) this, tag);
    }
}
