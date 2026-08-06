package com.toroidalworld.accessors;

import com.toroidalworld.core.WorldLoopTransformer;

// A level's transformer is decided once by its chunk generator and never replaced, so the level holds it in a field
// after the first resolve — every transformerOf call in the mod becomes a field read instead of an attachment-map
// lookup. Resolution stays lazy: the generator does not exist yet while the level is being constructed, and a resolve
// that early would memoize NOOP for good.
public interface TransformerCache {
    WorldLoopTransformer toroidal$transformer();
}
