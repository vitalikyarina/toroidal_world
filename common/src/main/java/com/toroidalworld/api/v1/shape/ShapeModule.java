package com.toroidalworld.api.v1.shape;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.shape.WorldShape;
import com.toroidalworld.shape.WorldShapes;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.WorldDimensions;

/**
 * How a world shape is declared: an id, the settings it starts from, what it does to a world's dimensions at creation,
 * and how it reads itself back out of an existing world. Build one with {@link #of} and call {@link #register()} at
 * startup, before the registration boundary closes; the shape then appears in the World Shape list on the create-world
 * screen, under the translation keys {@code gui.<namespace>.world_shape.<path>} and that plus {@code .hint}.
 *
 * <p>The module also holds the settings the player chose, which is why {@link #settings()} is what a Customize screen
 * reads and {@link #settings(Object)} what it writes back. Nothing here is thread-safe: it is create-screen state,
 * written on the client thread and read once as the world is made.</p>
 *
 * @param <S> the shape's own settings type, opaque to Toroidal World
 */
public final class ShapeModule<S> {

    /** What the shape does to the dimensions at creation — normally one {@link ShapeDimensions#withSpans} call. */
    @FunctionalInterface
    public interface Apply<S> {
        WorldDimensions apply(WorldDimensions dimensions, S settings);
    }

    /**
     * Reads the shape's settings back out of an existing world, or answers {@code null} when those dimensions are not
     * this shape's. The first shape that answers non-null owns the world, so a shape must refuse anything it did not
     * write — {@link ShapeDimensions#spansOf} plus a check of which axes loop is normally enough.
     */
    @FunctionalInterface
    public interface Read<S> {
        @Nullable S read(WorldDimensions dimensions);
    }

    private final Identifier id;
    private final S defaultSettings;
    private final Apply<S> apply;
    private final Read<S> read;

    private S settings;

    private ShapeModule(Identifier id, S defaultSettings, Apply<S> apply, Read<S> read) {
        this.id = id;
        this.defaultSettings = defaultSettings;
        this.apply = apply;
        this.read = read;
        this.settings = defaultSettings;
    }

    /**
     * Declares a shape. {@code id} names it and gives it its translation keys; {@code defaultSettings} is what the
     * create screen starts from and what a reset returns to.
     */
    public static <S> ShapeModule<S> of(Identifier id, S defaultSettings, Apply<S> apply, Read<S> read) {
        return new ShapeModule<>(id, defaultSettings, apply, read);
    }

    public Identifier id() {
        return this.id;
    }

    public S settings() {
        return this.settings;
    }

    public void settings(S chosen) {
        this.settings = chosen;
    }

    /**
     * Enrols the shape in the World Shape list.
     *
     * @throws IllegalStateException if the registration boundary has already closed
     */
    public void register() {
        WorldShapes.register(WorldShape.of(
                this.id,
                WorldShape.label(this.id),
                WorldShape.hint(this.id),
                this::applyAtCreation,
                this::resetSettings,
                this::restoreFromExisting));
    }

    private void resetSettings() {
        this.settings = this.defaultSettings;
    }

    private boolean restoreFromExisting(RegistryAccess.Frozen registries, WorldDimensions dimensions) {
        S restored = this.read.read(dimensions);
        if (restored == null) {
            return false;
        }

        this.settings = restored;
        return true;
    }

    private WorldDimensions applyAtCreation(RegistryAccess.Frozen registries, WorldDimensions dimensions) {
        return this.apply.apply(dimensions, this.settings);
    }
}
