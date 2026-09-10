/**
 * The public, semver-stable surface of Toroidal World for other mods.
 *
 * <p>Everything under {@code com.toroidalworld.api.v1} follows semantic versioning against the mod version: existing
 * members are not removed or changed incompatibly within a major version. Everything outside this package is
 * internal — it moves without notice, and mixins into it are unsupported.</p>
 *
 * <p>Reading a world's shape: {@link com.toroidalworld.api.v1.ToroidalWorldApi#shapeOf} for a server/logical-server
 * level, {@link com.toroidalworld.api.v1.ToroidalWorldClientApi#shapeOf} for the client level (bounds synced from the
 * server). Both hand back a {@link com.toroidalworld.api.v1.ToroidalShape}.</p>
 *
 * <p>Declaring one — added in 0.17.0, three surfaces a mod reads once:</p>
 * <ul>
 *   <li>{@code api.v1.shape} — {@link com.toroidalworld.api.v1.shape.ShapeModule} is a whole world shape, declaring
 *       its geometry as {@link com.toroidalworld.api.v1.shape.LoopSpans} through
 *       {@link com.toroidalworld.api.v1.shape.ShapeDimensions}.</li>
 *   <li>{@code api.v1.option} — {@link com.toroidalworld.api.v1.option.WorldOption} is a value the player picks at
 *       creation and the world stores, held in a {@link com.toroidalworld.api.v1.option.GenerationOptions}.</li>
 *   <li>{@code api.v1.gen} — {@link com.toroidalworld.api.v1.gen.GenerationHooks} runs a mod's own code at a named
 *       moment of world generation.</li>
 * </ul>
 *
 * <p>{@code api.v1.client} carries the client half of both declarations: the settings screen behind a shape's
 * Customize button, and the control one option is edited through.</p>
 *
 * <p>The walkthrough for all of it is {@code docs/modding.md}.</p>
 */
package com.toroidalworld.api.v1;
