package com.toroidalworld.api.v1.gen;

/**
 * What the carvers tail recorded about one chunk's terrain: which cells the generator had filled by then, and which
 * of those a later step has since written over. A pass that removes terrain may take only what still stands in both
 * readings, which is what keeps it off the blocks features and structures put down.
 *
 * <p>Opaque by design — a harness holds one per chunk and hands the nine of a window back to
 * {@link GenerationProbe#borderCrumbs}. One is obtained from {@link GenerationProbe#snapshotOf} after that chunk has
 * been through {@link GenerationProbe#sweepCrumbs}; a chunk that never was carries none.</p>
 */
public interface TerrainSnapshot {
}
