package com.toroidalworld.accessors;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldLoopTransformer;

public interface TransformerSource {
    @Nullable
    WorldLoopTransformer toroidal$wrappedTransformer();
}
