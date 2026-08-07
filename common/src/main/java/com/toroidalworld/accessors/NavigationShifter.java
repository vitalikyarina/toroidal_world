package com.toroidalworld.accessors;

// A seam wrap moves a mob a whole world in raw coordinates while its steering keeps state captured before the jump:
// the navigation's path nodes, remembered target and stuck/timeout anchors, and the move/look controls' wanted point.
// Vanilla compares all of it against the mob's position raw — the follow chain's advance checks and safety nets, and
// MoveControl.tick, which consumes a pending wanted with a plain difference and turns the mob up to 90° toward the
// old-space copy for a tick: one wrong step at the line re-crosses the seam, and the mob jitters in circles on the
// boundary. The invariant to restore is that a mob and everything steering it share one continuous coordinate space:
// whoever wraps the mob shifts each of these holders by the same whole-world vector in the same moment.
public interface NavigationShifter {
    void toroidal$shiftBy(int shiftX, int shiftZ);
}
