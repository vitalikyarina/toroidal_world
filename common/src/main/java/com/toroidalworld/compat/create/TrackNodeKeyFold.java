package com.toroidalworld.compat.create;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.level.Level;

// Folding a track node's key into the world's bounds, asked of the node itself. The coordinates live in Vec3i, whose
// setters only a subclass may reach, so the statement is written into TrackNodeLocation by its mixin and offered here
// to the three other places that build a node and know its dimension one step later than its constructor does.
public interface TrackNodeKeyFold {
    // The level when the caller has one — a client level answers by the bounds the server sent, which is the only
    // reading available on a client with no integrated server. Null leaves the node's own dimension to find it.
    void toroidal$foldNodeKey(@Nullable Level level);
}
