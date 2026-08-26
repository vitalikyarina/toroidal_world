package com.toroidalworld.client.shape.cylinder;

import java.util.Locale;
import java.util.function.Consumer;

import com.toroidalworld.client.shape.LoopSizeControls;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.shape.cylinder.CylinderSettings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class CylinderSettingsScreen extends Screen {
    private static final Component TITLE = Component.translatable("gui.toroidal_world.cylinder_settings.title");
    private static final Component AXIS_LABEL = Component.translatable("gui.toroidal_world.cylinder_settings.axis");
    private static final Component AXIS_HINT = Component.translatable("gui.toroidal_world.cylinder_settings.axis_hint");

    private static final int FOOTER_SPACING = 8;
    private static final int CONTENTS_SPACING = 8;

    private final Screen parent;
    private final Consumer<CylinderSettings> onDone;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final LoopSizeControls controls;

    private Direction.Axis axis;

    private Button doneButton;

    public CylinderSettingsScreen(Screen parent, CylinderSettings current, Consumer<CylinderSettings> onDone) {
        super(TITLE);
        this.parent = parent;
        this.onDone = onDone;
        this.axis = current.axis();
        this.controls = new LoopSizeControls(current.chunkWidth(), current.netherScale(), current.endChunkWidth(),
                this::refreshDoneButton);
    }

    @Override
    protected void init() {
        this.layout.addTitleHeader(TITLE, this.font);

        LinearLayout contents = this.layout.addToContents(LinearLayout.vertical().spacing(CONTENTS_SPACING));

        contents.addChild(CycleButton.builder(CylinderSettingsScreen::axisName, this.axis)
                .withValues(Direction.Axis.X, Direction.Axis.Z)
                .withTooltip(chosen -> Tooltip.create(AXIS_HINT))
                .create(0, 0, LoopSizeControls.FIELD_WIDTH, LoopSizeControls.FIELD_HEIGHT, AXIS_LABEL,
                        (button, chosen) -> this.axis = chosen));

        this.controls.addTo(this.font, contents);

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

        this.onDone.accept(new CylinderSettings(
                WorldLoopBounds.ofWidth(this.axis, this.controls.effectiveSize()),
                this.controls.netherScale(),
                WorldLoopBounds.ofWidth(this.axis, this.controls.effectiveEndSize())));
        this.onClose();
    }

    private static Component axisName(Direction.Axis axis) {
        return Component.literal(axis.getName().toUpperCase(Locale.ROOT));
    }
}
