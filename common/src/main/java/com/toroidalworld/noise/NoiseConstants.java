package com.toroidalworld.noise;

public final class NoiseConstants {
    public static final double SHIFT_SCALE = 0.25;

    // Horizontal scales vanilla bakes into the coordinates it hands to the surface noises (SurfaceSystem).
    public static final double BADLANDS_PILLAR_SCALE = 0.2;
    public static final double BADLANDS_PILLAR_ROOF_SCALE = 0.75;
    public static final double ICEBERG_PILLAR_SCALE = 1.28;
    public static final double ICEBERG_PILLAR_ROOF_SCALE = 1.17;

    // NormalNoise.INPUT_FACTOR (private there): vanilla detunes the second Perlin layer by this factor so the two
    // lattices never resonate. On a wrapped world it travels through the context scale, giving the second layer its
    // own period.
    public static final double SECOND_LAYER_DETUNE = 1.0181268882175227;

    public static final double UNSCALED = 1.0;

    private NoiseConstants() {
    }
}
