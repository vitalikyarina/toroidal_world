package com.toroidalworld.client.shape;

import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public abstract class LoopSettingsScreen<S> extends Screen {
    private static final int FOOTER_SPACING = 8;
    private static final int CONTENTS_SPACING = 8;

    protected final LoopSizeControls controls;

    private final Screen parent;
    private final Consumer<S> onDone;

    private HeaderAndFooterLayout layout;
    private ScrollableLayout contentsScroll;
    private Button doneButton;

    protected LoopSettingsScreen(Component title, Screen parent, int chunkWidth, int netherScale, int endChunkWidth,
            Consumer<S> onDone) {
        super(title);
        this.parent = parent;
        this.onDone = onDone;
        this.controls = new LoopSizeControls(chunkWidth, netherScale, endChunkWidth, this::onControlsChanged);
    }

    protected abstract S build();

    protected void addBeforeFields(Font font, LinearLayout contents) {
    }

    protected void addAfterFields(Font font, LinearLayout contents) {
    }

    protected boolean isComplete() {
        return this.controls.isComplete();
    }

    protected void onControlsChanged() {
        this.refreshDoneButton();
    }

    protected final Screen parent() {
        return this.parent;
    }

    protected final void refreshDoneButton() {
        this.doneButton.active = this.isComplete();
    }

    @Override
    protected void init() {
        this.layout = new HeaderAndFooterLayout(this);
        this.layout.addTitleHeader(this.title, this.font);

        LinearLayout contents = LinearLayout.vertical().spacing(CONTENTS_SPACING);
        this.controls.addPresets(contents);
        this.addBeforeFields(this.font, contents);
        this.controls.addFields(this.font, contents);
        this.addAfterFields(this.font, contents);

        this.contentsScroll = new ScrollableLayout(this.minecraft, contents, this.layout.getContentHeight());
        this.layout.addToContents(this.contentsScroll);

        LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(FOOTER_SPACING));
        this.doneButton = footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> this.commit()).build());
        footer.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose()).build());

        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
        this.controls.refresh();
    }

    @Override
    protected void repositionElements() {
        this.contentsScroll.arrangeElements();
        this.contentsScroll.setMaxHeight(this.layout.getContentHeight());
        this.layout.arrangeElements();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(this.parent);
    }

    private void commit() {
        if (!this.isComplete()) {
            return;
        }

        this.onDone.accept(this.build());
        this.onClose();
    }
}
