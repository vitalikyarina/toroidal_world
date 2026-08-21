package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.toroidalworld.accessors.RelocatableBlockEntity;
import com.toroidalworld.accessors.SeamKeyedBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

@Mixin(BlockEntity.class)
public class BlockEntityMixin implements RelocatableBlockEntity {
    @Mutable
    @Shadow
    @Final
    protected BlockPos worldPosition;

    @Override
    public void toroidal$relocate(BlockPos pos) {
        this.worldPosition = pos.immutable();
    }

    @Inject(method = "setLevel", at = @At("TAIL"))
    private void toroidal$rekeyStoredPositions(Level level, CallbackInfo ci) {
        if (this instanceof SeamKeyedBlockEntity keyed) {
            keyed.toroidal$rekey();
        }
    }
}
