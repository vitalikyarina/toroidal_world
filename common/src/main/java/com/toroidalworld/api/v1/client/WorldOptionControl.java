package com.toroidalworld.api.v1.client;

import com.toroidalworld.api.v1.option.GenerationOptions;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.layouts.LinearLayout;

/**
 * What one world option contributes to a settings screen: its widgets, and the value it writes back. A control is
 * built once per screen, through the factory registered with {@link WorldOptionControls#register}, and reaches the
 * screen around it only through its {@link WorldOptionContext}.
 */
public interface WorldOptionControl {

    /** Adds this option's widgets to the screen's vertical contents, in registry order with the other options. */
    void addWidgets(Font font, LinearLayout contents);

    /** The options with this control's current value written in; called when the screen's Done is pressed. */
    GenerationOptions commit(GenerationOptions options);

    /** Called when the screen state a control may read — the loop width — has changed under it. */
    default void onSharedStateChanged() {
    }

    /** Whether this control's value is usable; a screen keeps Done inactive while any control answers {@code false}. */
    default boolean isComplete() {
        return true;
    }
}
