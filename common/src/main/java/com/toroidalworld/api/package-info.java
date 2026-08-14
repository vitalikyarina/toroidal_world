/**
 * The public, semver-stable surface of Toroidal World for other mods.
 *
 * <p>Everything under {@code com.toroidalworld.api} follows semantic versioning against the mod version: existing
 * members are not removed or changed incompatibly within a major version. Everything outside this package is
 * internal — it moves without notice, and mixins into it are unsupported.</p>
 *
 * <p>Entry points: {@link com.toroidalworld.api.ToroidalWorldApi#shapeOf} for a server/logical-server level,
 * {@link com.toroidalworld.api.ToroidalWorldClientApi#shapeOf} for the client level (bounds synced from the
 * server).</p>
 */
package com.toroidalworld.api;
