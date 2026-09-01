package com.toroidalworld.client.shape.torus;

import com.toroidalworld.client.shape.LoopSizeControls;
import com.toroidalworld.options.WorldLoopBounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class TorusSettingsScreen extends Screen {
    private static final Component TITLE = Component.translatable("gui.toroidal_world.toroidal_settings.title");

    private static final int FOOTER_SPACING = 8;
    private static final int CONTENTS_SPACING = 8;

    private final Screen parent;
    private final OnDone onDone;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final LoopSizeControls controls;

    private Button doneButton;

    public TorusSettingsScreen(Screen parent, WorldLoopBounds current, int currentNetherScale,
            WorldLoopBounds currentEnd, OnDone onDone) {
        super(TITLE);
        this.parent = parent;
        this.onDone = onDone;
        this.controls = new LoopSizeControls(current.chunkWidth(), currentNetherScale, currentEnd.chunkWidth(),
                this::refreshDoneButton);
    }

    @Override
    protected void init() {
        this.layout.addTitleHeader(TITLE, this.font);

        LinearLayout contents = this.layout.addToContents(LinearLayout.vertical().spacing(CONTENTS_SPACING));
        this.controls.addPresets(contents);
        this.controls.addFields(this.font, contents);

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

    private void refreshDoneButton() {
        this.doneButton.active = this.controls.isComplete();
    }

    private void commit() {
        if (!this.controls.isComplete()) {
            return;
        }

        this.onDone.accept(WorldLoopBounds.ofWidth(this.controls.effectiveSize()), this.controls.netherScale(),
                WorldLoopBounds.ofWidth(this.controls.effectiveEndSize()));
        this.onClose();
    }

    @FunctionalInterface
    public interface OnDone {
        void accept(WorldLoopBounds wrapping, int netherScale, WorldLoopBounds endWrapping);
    }
}
