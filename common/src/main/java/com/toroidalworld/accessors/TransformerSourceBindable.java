package com.toroidalworld.accessors;

// For an object that never sees a level and never will, but is handed one that already answers the question. Unlike
// LevelBindable it carries the resolver rather than the level: the binder here has already paid for the resolve and
// caches it, so a second lookup path would be a second answer to keep in step.
public interface TransformerSourceBindable {
    default void toroidal$bindTransformerSource(TransformerSource source) {
    }
}
