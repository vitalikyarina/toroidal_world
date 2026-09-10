package com.toroidalworld.client.shape.torus;

import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.api.v1.client.WorldOptionContext;
import com.toroidalworld.api.v1.client.WorldOptionControl;
import com.toroidalworld.api.v1.client.WorldOptionControls;
import com.toroidalworld.client.shape.LoopSettingsScreen;
import com.toroidalworld.api.v1.option.GenerationOptions;
import com.toroidalworld.api.v1.shape.LoopSpans;
import com.toroidalworld.shape.torus.TorusSettings;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class TorusSettingsScreen extends LoopSettingsScreen<TorusSettings> {
    private static final Component TITLE = Component.translatable("gui.toroidal_world.toroidal_settings.title");
    private static final Component ADDITIONAL_SECTION =
            Component.translatable("gui.toroidal_world.toroidal_settings.section.additional")
                    .withStyle(ChatFormatting.BOLD);

    private final GenerationOptions generationOptions;
    private final List<WorldOptionControl> optionControls;

    public TorusSettingsScreen(Screen parent, TorusSettings current, Consumer<TorusSettings> onDone) {
        super(TITLE, parent, current.chunkWidth(), current.netherScale(), current.endChunkWidth(),
                onDone);
        this.generationOptions = current.generationOptions();
        this.optionControls = WorldOptionControls.createAll(new ScreenContext());
    }

    @Override
    protected void addAfterFields(Font font, LinearLayout contents) {
        contents.addChild(new StringWidget(ADDITIONAL_SECTION, font), LayoutSettings::alignHorizontallyCenter);

        for (WorldOptionControl control : this.optionControls) {
            control.addWidgets(font, contents);
        }
    }

    @Override
    protected boolean isComplete() {
        return super.isComplete() && this.optionControls.stream().allMatch(WorldOptionControl::isComplete);
    }

    @Override
    protected void onControlsChanged() {
        super.onControlsChanged();

        for (WorldOptionControl control : this.optionControls) {
            control.onSharedStateChanged();
        }
    }

    @Override
    protected TorusSettings build() {
        GenerationOptions chosen = this.generationOptions;
        for (WorldOptionControl control : this.optionControls) {
            chosen = control.commit(chosen);
        }

        return new TorusSettings(
                LoopSpans.ofWidth(this.controls.effectiveSize()),
                this.controls.netherScale(),
                LoopSpans.ofWidth(this.controls.effectiveEndSize()),
                chosen);
    }

    private final class ScreenContext implements WorldOptionContext {
        @Override
        public Screen parent() {
            return TorusSettingsScreen.this.parent();
        }

        @Override
        public @Nullable Integer loopChunkWidth() {
            return TorusSettingsScreen.this.controls.effectiveSize();
        }

        @Override
        public GenerationOptions options() {
            return TorusSettingsScreen.this.generationOptions;
        }

        @Override
        public void onChanged() {
            TorusSettingsScreen.this.refreshDoneButton();
        }

        @Override
        public void rebuild() {
            TorusSettingsScreen.this.rebuildWidgets();
        }
    }
}
