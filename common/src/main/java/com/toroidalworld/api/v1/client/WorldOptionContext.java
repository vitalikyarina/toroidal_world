package com.toroidalworld.api.v1.client;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.api.v1.option.GenerationOptions;

import net.minecraft.client.gui.screens.Screen;

/**
 * All a {@link WorldOptionControl} may reach of the settings screen around it, so that no control holds the screen
 * itself. A shape's own settings screen implements this and hands it to {@link WorldOptionControls#createAll}.
 */
public interface WorldOptionContext {

    /** The screen the control lives on — what a sub-screen it opens returns to. */
    Screen parent();

    /** The loop width the screen currently states, in chunks, or {@code null} while it states none. */
    @Nullable Integer loopChunkWidth();

    /** The options the screen was opened with. */
    GenerationOptions options();

    /** Tells the screen a control's value changed, so it can re-check whether Done is available. */
    void onChanged();

    /** Asks the screen to lay its widgets out again — for a control whose widget set depends on its own value. */
    void rebuild();
}
