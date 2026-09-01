package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.storage.SectionStorage;

@Mixin(SectionStorage.class)
public interface SectionStorageAccessor {
    @Accessor("levelHeightAccessor")
    LevelHeightAccessor toroidal$getLevelHeightAccessor();
}
