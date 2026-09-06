package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.toroidalworld.accessors.ShapeStamp;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.options.GenerationOptions;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.world.level.chunk.ChunkGenerator;

@Mixin(ChunkGenerator.class)
public class ChunkGeneratorMixin implements ShapeStamp {
    @Unique
    private volatile @Nullable FlatShape toroidal$stampedShape;

    @Unique
    private volatile @Nullable WorldFold toroidal$stampedTransformer;

    @Unique
    private volatile GenerationOptions toroidal$stampedGenerationOptions = GenerationOptions.DEFAULT;

    @Override
    public @Nullable FlatShape toroidal$stampedShape() {
        return this.toroidal$stampedShape;
    }

    @Override
    public @Nullable WorldFold toroidal$stampedTransformer() {
        return this.toroidal$stampedTransformer;
    }

    @Override
    public GenerationOptions toroidal$stampedGenerationOptions() {
        return this.toroidal$stampedGenerationOptions;
    }

    @Override
    public void toroidal$stamp(FlatShape shape, GenerationOptions generationOptions) {
        this.toroidal$stampedShape = shape;
        this.toroidal$stampedGenerationOptions = generationOptions;
        this.toroidal$stampedTransformer = WorldFolds.of(shape, generationOptions);
    }

    @Override
    public void toroidal$clearStamp() {
        this.toroidal$stampedShape = null;
        this.toroidal$stampedGenerationOptions = GenerationOptions.DEFAULT;
        this.toroidal$stampedTransformer = null;
    }
}
