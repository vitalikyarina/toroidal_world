package com.toroidalworld.client.shape.torus;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.client.options.WorldOptionContext;
import com.toroidalworld.client.options.WorldOptionControl;
import com.toroidalworld.client.options.WorldOptionControls;
import com.toroidalworld.client.shape.LoopSizeControls;
import com.toroidalworld.core.GenerationOptions;
import com.toroidalworld.core.WorldLoopBounds;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class TorusSettingsScreen extends Screen {
    private static final Component TITLE = Component.translatable("gui.toroidal_world.toroidal_settings.title");
    private static final Component ADDITIONAL_SECTION =
            Component.translatable("gui.toroidal_world.toroidal_settings.section.additional")
                    .withStyle(ChatFormatting.BOLD);

    private static final int FOOTER_SPACING = 8;
    private static final int CONTENTS_SPACING = 8;

    private final Screen parent;
    private final OnDone onDone;
    private final LoopSizeControls controls;
    private final GenerationOptions generationOptions;
    private final List<WorldOptionControl> optionControls;

    private HeaderAndFooterLayout layout;
    private Button doneButton;

    public TorusSettingsScreen(Screen parent, WorldLoopBounds current, int currentNetherScale,
            WorldLoopBounds currentEnd, GenerationOptions currentOptions, OnDone onDone) {
        super(TITLE);
        this.parent = parent;
        this.onDone = onDone;
        this.generationOptions = currentOptions;
        this.controls = new LoopSizeControls(current.chunkWidth(), currentNetherScale, currentEnd.chunkWidth(),
                this::onControlsChanged);
        this.optionControls = WorldOptionControls.createAll(new ScreenContext());
    }

    @Override
    protected void init() {
        this.layout = new HeaderAndFooterLayout(this);
        this.layout.addTitleHeader(TITLE, this.font);

        LinearLayout contents = this.layout.addToContents(LinearLayout.vertical().spacing(CONTENTS_SPACING));
        this.controls.addPresets(contents);
        this.controls.addFields(this.font, contents);

        contents.addChild(new StringWidget(ADDITIONAL_SECTION, this.font),
                LayoutSettings::alignHorizontallyCenter);

        for (WorldOptionControl control : this.optionControls) {
            control.addWidgets(this.font, contents);
        }

        LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(FOOTER_SPACING));
        this.doneButton = footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> this.commit()).build());
        footer.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose()).build());

        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
        this.controls.refresh();
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(this.parent);
    }

    private void onControlsChanged() {
        this.refreshDoneButton();
        for (WorldOptionControl control : this.optionControls) {
            control.onSharedStateChanged();
        }
    }

    private boolean isComplete() {
        return this.controls.isComplete()
                && this.optionControls.stream().allMatch(WorldOptionControl::isComplete);
    }

    private void refreshDoneButton() {
        this.doneButton.active = this.isComplete();
    }

    private void commit() {
        if (!this.isComplete()) {
            return;
        }

        GenerationOptions chosen = this.generationOptions;
        for (WorldOptionControl control : this.optionControls) {
            chosen = control.commit(chosen);
        }

        this.onDone.accept(WorldLoopBounds.ofWidth(this.controls.effectiveSize()), this.controls.netherScale(),
                WorldLoopBounds.ofWidth(this.controls.effectiveEndSize()), chosen);
        this.onClose();
    }

    private final class ScreenContext implements WorldOptionContext {
        @Override
        public Screen parent() {
            return TorusSettingsScreen.this.parent;
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

    @FunctionalInterface
    public interface OnDone {
        void accept(WorldLoopBounds wrapping, int netherScale, WorldLoopBounds endWrapping,
                GenerationOptions generationOptions);
    }
}
