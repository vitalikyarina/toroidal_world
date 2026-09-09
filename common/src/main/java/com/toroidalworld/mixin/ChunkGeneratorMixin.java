package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.toroidalworld.accessors.ShapeStamp;
import com.toroidalworld.core.CarriedShape;

import net.minecraft.world.level.chunk.ChunkGenerator;

@Mixin(ChunkGenerator.class)
public class ChunkGeneratorMixin implements ShapeStamp {
    @Unique
    private volatile @Nullable CarriedShape toroidal$carriedShape;

    @Override
    public @Nullable CarriedShape toroidal$carriedShape() {
        return this.toroidal$carriedShape;
    }

    @Override
    public void toroidal$stamp(CarriedShape carried) {
        this.toroidal$carriedShape = carried;
    }

    @Override
    public void toroidal$clearStamp() {
        this.toroidal$carriedShape = null;
    }
}
