package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.toroidalworld.accessors.ShapeStamp;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.options.ClimateScale;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.world.level.chunk.ChunkGenerator;

@Mixin(ChunkGenerator.class)
public class ChunkGeneratorMixin implements ShapeStamp {
    @Unique
    private volatile @Nullable FlatShape toroidal$stampedShape;

    @Unique
    private volatile @Nullable WorldFold toroidal$stampedTransformer;

    @Unique
    private volatile ClimateScale toroidal$stampedClimateScale = WorldFolds.CLIMATE_SCALE_DEFAULT;

    @Override
    public @Nullable FlatShape toroidal$stampedShape() {
        return this.toroidal$stampedShape;
    }

    @Override
    public @Nullable WorldFold toroidal$stampedTransformer() {
        return this.toroidal$stampedTransformer;
    }

    @Override
    public ClimateScale toroidal$stampedClimateScale() {
        return this.toroidal$stampedClimateScale;
    }

    @Override
    public void toroidal$stamp(FlatShape shape, ClimateScale climateScale) {
        this.toroidal$stampedShape = shape;
        this.toroidal$stampedClimateScale = climateScale;
        this.toroidal$stampedTransformer = WorldFolds.of(shape, climateScale);
    }

    @Override
    public void toroidal$clearStamp() {
        this.toroidal$stampedShape = null;
        this.toroidal$stampedClimateScale = WorldFolds.CLIMATE_SCALE_DEFAULT;
        this.toroidal$stampedTransformer = null;
    }
}
