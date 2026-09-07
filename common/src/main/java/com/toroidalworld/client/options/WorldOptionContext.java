package com.toroidalworld.client.options;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.options.GenerationOptions;

import net.minecraft.client.gui.screens.Screen;

public interface WorldOptionContext {
    Screen parent();

    @Nullable Integer loopChunkWidth();

    GenerationOptions options();

    void onChanged();

    void rebuild();
}
