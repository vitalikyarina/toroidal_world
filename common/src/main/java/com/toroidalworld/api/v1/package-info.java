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
 * <p>Making a mechanism of your own work on one — added in 0.17.0. Every vanilla packet carrying a world position
 * is already rewritten between the server's frame and the client's; a payload, a particle type or a rigid group
 * this mod never heard of is not, because nothing but its author knows where a position sits inside it:</p>
 * <ul>
 *   <li>{@code api.v1.net} — {@link com.toroidalworld.api.v1.net.PacketRewriters} is where a mod says how its own
 *       payloads and particle types cross, through the {@link com.toroidalworld.api.v1.net.SeamContext} of the
 *       connection each one is on.</li>
 *   <li>{@link com.toroidalworld.api.v1.SeamShift}, taken from
 *       {@link com.toroidalworld.api.v1.ToroidalShape#shiftToNearestCopy}, carries a whole rigid group into one
 *       copy of the world — seating its members one by one tears it in half.</li>
 *   <li>{@link com.toroidalworld.api.v1.client.ClientAnchors} seats a position against the anchors a client
 *       already has: its player, its camera, and the copy of a position it is actually holding.</li>
 * </ul>
 *
 * <p>The walkthrough for all of it is {@code docs/modding.md}.</p>
 */
package com.toroidalworld.api.v1;
