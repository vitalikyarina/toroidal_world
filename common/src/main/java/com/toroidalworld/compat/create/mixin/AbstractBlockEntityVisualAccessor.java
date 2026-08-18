package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;

import net.minecraft.world.level.block.entity.BlockEntity;

// The block entity a Flywheel visual was built for, which is the only route from inside GearboxVisual to the level and
// the position its fold needs. @Shadow cannot reach either: both live on Flywheel's base classes rather than on the
// Create subclass the mixin targets, and a shadow resolves against the target class alone — the game says so at apply
// time, with "@Shadow field level was not located in the target class".
@Mixin(value = AbstractBlockEntityVisual.class, remap = false)
public interface AbstractBlockEntityVisualAccessor {
    @Accessor("blockEntity")
    BlockEntity toroidal$blockEntity();
}
