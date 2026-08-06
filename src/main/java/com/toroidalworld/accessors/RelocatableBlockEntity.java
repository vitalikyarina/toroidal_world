package com.toroidalworld.accessors;

import net.minecraft.core.BlockPos;

// A block entity's position is its identity — the key it is stored under, the key its ticker is registered under, and
// what it writes to NBT. Vanilla fixes that identity in the constructor, where there is no level yet and so no way to
// know the world wraps. This lets the level correct it at the one moment it can: when the entity is handed to the world.
public interface RelocatableBlockEntity {
    void toroidal$relocate(BlockPos pos);
}
