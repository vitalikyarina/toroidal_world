package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.toroidalworld.compat.create.CreateMultiblockFold;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

@Mixin(value = SmartBlockEntity.class, remap = false)
public class SmartBlockEntityControllerMixin {
    @ModifyVariable(method = { "loadAdditional", "readClient" }, at = @At("HEAD"), argsOnly = true)
    private CompoundTag toroidal$foldControllerPairIntoOwnFrame(CompoundTag tag) {
        if (!(this instanceof IMultiBlockEntityContainer)) {
            return tag;
        }

        return CreateMultiblockFold.controllerPairInFrameOf((BlockEntity) (Object) this, tag);
    }
}
