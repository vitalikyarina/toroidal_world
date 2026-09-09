package com.toroidalworld.shape;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.WorldDimensions;

public final class ShapeModule<S> {
    @FunctionalInterface
    public interface Apply<S> {
        WorldDimensions apply(WorldDimensions dimensions, S settings);
    }

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
