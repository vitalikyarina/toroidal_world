package com.toroidalworld.client.options;

import com.toroidalworld.options.GenerationOptions;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.layouts.LinearLayout;

public interface WorldOptionControl {
    void addWidgets(Font font, LinearLayout contents);

    GenerationOptions commit(GenerationOptions options);

    default void onSharedStateChanged() {
    }

    default boolean isComplete() {
        return true;
    }
}
