package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import com.toroidalworld.accessors.RelocatableBlockEntity;

import net.minecraft.core.BlockPos;
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
}
