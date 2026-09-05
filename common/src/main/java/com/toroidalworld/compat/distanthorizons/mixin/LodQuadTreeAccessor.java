package com.toroidalworld.compat.distanthorizons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.seibel.distanthorizons.core.level.IDhClientLevel;
import com.seibel.distanthorizons.core.render.QuadTree.LodQuadTree;

@Mixin(LodQuadTree.class)
public interface LodQuadTreeAccessor {
    @Accessor("level")
    IDhClientLevel toroidal$level();
}
