package com.toroidalworld.noise;

public final class NoiseConstants {
    public static final double SHIFT_SCALE = 0.25;

    // Horizontal scales vanilla bakes into the coordinates it hands the surface noises (SurfaceSystem).
    public static final double BADLANDS_PILLAR_SCALE = 0.2;
    public static final double BADLANDS_PILLAR_ROOF_SCALE = 0.75;
    public static final double ICEBERG_PILLAR_SCALE = 1.28;
    public static final double ICEBERG_PILLAR_ROOF_SCALE = 1.17;

    // The same for Biome's climate noises: ice-patch body, edge, interior, then height-adjusted temperature.
    public static final double FROZEN_TEMPERATURE_SCALE = 0.05;
    public static final double BIOME_INFO_EDGE_SCALE = 0.2;
    public static final double BIOME_INFO_PATCH_SCALE = 0.09;
    public static final double HEIGHT_TEMPERATURE_SCALE = 1.0 / 8.0;

    // NormalNoise.INPUT_FACTOR, private there: it detunes the second Perlin layer so the two lattices never resonate.
    public static final double SECOND_LAYER_DETUNE = 1.0181268882175227;

    public static final double UNSCALED = 1.0;

    private NoiseConstants() {
    }
}
