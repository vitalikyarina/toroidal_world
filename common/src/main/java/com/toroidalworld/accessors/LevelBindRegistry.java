package com.toroidalworld.accessors;

// A bind relay for graph parts whose classes cannot be named from outside their package (LoadingChunkTracker is
// package-private): the part registers itself here at construction, and the host forwards its own bind to every
// registered part.
public interface LevelBindRegistry {
    default void toroidal$registerBindable(LevelBindable bindable) {
    }
}
