package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.storage.SectionStorage;

// The height accessor is declared on SectionStorage, not on PoiManager, so a @Shadow of it from the PoiManager mixin
// cannot see it (shadow fields resolve only in the target class itself). Exposed from where it lives — for a server POI
// manager it is the ServerLevel, which is the route to the transformer.
@Mixin(SectionStorage.class)
public interface SectionStorageAccessor {
    @Accessor("levelHeightAccessor")
    LevelHeightAccessor toroidal$getLevelHeightAccessor();
}
