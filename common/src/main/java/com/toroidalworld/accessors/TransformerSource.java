package com.toroidalworld.accessors;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldFold;

public interface TransformerSource {
    @Nullable
    WorldFold toroidal$wrappedTransformer();
}
