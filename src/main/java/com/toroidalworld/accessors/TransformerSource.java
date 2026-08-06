package com.toroidalworld.accessors;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldLoopTransformer;

// An entity already resolves the transformer of the level it stands in and keeps it until it changes level. Everything
// that acts on an entity — its look and move controls, its navigation, the tracker entry that decides who is shown it —
// reaches the same answer through the entity instead of asking the level's attachment map again per call.
public interface TransformerSource {
    @Nullable
    WorldLoopTransformer toroidal$wrappedTransformer();
}
