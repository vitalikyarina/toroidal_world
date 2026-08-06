package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import com.toroidalworld.accessors.RelocatableBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

// The field is final in vanilla for a good reason — nothing may move a block entity once the world knows about it. The
// one caller here rewrites it strictly before registration, while the entity is still nobody's, so that invariant holds.
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
