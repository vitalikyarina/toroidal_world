package com.toroidalworld.client.shape.cylinder;

import java.util.Locale;
import java.util.function.Consumer;

import com.toroidalworld.client.shape.LoopSettingsScreen;
import com.toroidalworld.client.shape.LoopSizeControls;
import com.toroidalworld.core.WorldLoopBounds;
import com.toroidalworld.shape.cylinder.CylinderSettings;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

public class CylinderSettingsScreen extends LoopSettingsScreen<CylinderSettings> {
    private static final Component TITLE = Component.translatable("gui.toroidal_world.cylinder_settings.title");
    private static final Component AXIS_LABEL = Component.translatable("gui.toroidal_world.cylinder_settings.axis");
    private static final Component AXIS_HINT = Component.translatable("gui.toroidal_world.cylinder_settings.axis_hint");

    private Direction.Axis axis;

    public CylinderSettingsScreen(Screen parent, CylinderSettings current, Consumer<CylinderSettings> onDone) {
        super(TITLE, parent, current.chunkWidth(), current.netherScale(), current.endChunkWidth(), onDone);
        this.axis = current.axis();
    }

    @Override
    protected void addBeforeFields(Font font, LinearLayout contents) {
        contents.addChild(CycleButton.builder(CylinderSettingsScreen::axisName, this.axis)
                .withValues(Direction.Axis.X, Direction.Axis.Z)
                .withTooltip(chosen -> Tooltip.create(AXIS_HINT))
                .create(0, 0, LoopSizeControls.FIELD_WIDTH, LoopSizeControls.FIELD_HEIGHT, AXIS_LABEL,
                        (button, chosen) -> this.axis = chosen));
    }

    @Override
    protected CylinderSettings build() {
        return new CylinderSettings(
                WorldLoopBounds.ofWidth(this.axis, this.controls.effectiveSize()),
                this.controls.netherScale(),
                WorldLoopBounds.ofWidth(this.axis, this.controls.effectiveEndSize()));
    }

    private static Component axisName(Direction.Axis axis) {
        return Component.literal(axis.getName().toUpperCase(Locale.ROOT));
    }
}
