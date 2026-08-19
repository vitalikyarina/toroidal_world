package com.toroidalworld.compat.c2me;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.noise.GenerationTransformerContext.Context;

public final class C2meDfcFold {
    public static @Nullable Context wrappedContext() {
        Context context = GenerationTransformerContext.context();
        return context.transformer().isWrapped() ? context : null;
    }

    private C2meDfcFold() {
    }
}
