package com.toroidalworld.accessors;

import net.minecraft.world.phys.Vec3;

// GameEvent.ListenerInfo collapses the recipient's position into a raw distance in its constructor and keeps only the
// number, so the queue it sorts cannot be re-ordered from what the record carries. The position handed to that
// constructor is the exact value the distance was taken from; asking the listener's PositionSource again would be a
// second reading, not the one measured, and for an entity source not even reliably the same point.
public interface RecipientPositionHolder {
    Vec3 toroidal$recipientPos();
}
