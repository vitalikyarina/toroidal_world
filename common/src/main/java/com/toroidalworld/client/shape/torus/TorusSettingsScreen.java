package com.toroidalworld.client.shape.torus;

import java.util.EnumMap;
import java.util.Map;

import com.toroidalworld.client.shape.LoopSizeControls;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopPresets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class TorusSettingsScreen extends Screen {
    private static final Component TITLE = Component.translatable("gui.toroidal_world.toroidal_settings.title");

    private static final String PRESET_KEY_PREFIX = "gui.toroidal_world.toroidal_settings.preset.";
    private static final String STRUCTURES_SCARCE_KEY = "gui.toroidal_world.toroidal_settings.consequence.structures_scarce";
    private static final String STRUCTURES_VILLAGES_KEY = "gui.toroidal_world.toroidal_settings.consequence.structures_villages";
    private static final String STRUCTURES_COMMON_KEY = "gui.toroidal_world.toroidal_settings.consequence.structures_common";
    private static final String STRUCTURES_ALL_KEY = "gui.toroidal_world.toroidal_settings.consequence.structures_all";

    // Read off vanilla 26.2 StructureSets: densest common grids 34 chunks, mansions 80, the stronghold ring 168.
    private static final int VILLAGE_GRID_CHUNKS = 34;
    private static final int MANSION_GRID_CHUNKS = 80;
    private static final int STRONGHOLD_RING_CHUNKS = 336;

    private static final int FOOTER_SPACING = 8;
    private static final int CONTENTS_SPACING = 8;
    private static final int PRESET_SPACING = 5;

    private static final int PRESET_WIDTH =
            (LoopSizeControls.FIELD_WIDTH - PRESET_SPACING * (WorldLoopPresets.values().length - 1))
                    / WorldLoopPresets.values().length;

    private final Screen parent;
    private final OnDone onDone;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final LoopSizeControls controls;

    private Button doneButton;
    private final Map<WorldLoopPresets, Button> presetButtons = new EnumMap<>(WorldLoopPresets.class);

    public TorusSettingsScreen(Screen parent, WorldLoopBounds current, int currentNetherScale,
            WorldLoopBounds currentEnd, OnDone onDone) {
        super(TITLE);
        this.parent = parent;
        this.onDone = onDone;
        this.controls = new LoopSizeControls(current.chunkWidth(), currentNetherScale, currentEnd.chunkWidth(),
                this::onControlsChanged);
    }

    @Override
    protected void init() {
        this.layout.addTitleHeader(TITLE, this.font);

        LinearLayout contents = this.layout.addToContents(LinearLayout.vertical().spacing(CONTENTS_SPACING));

        LinearLayout presetRow = contents.addChild(LinearLayout.horizontal().spacing(PRESET_SPACING));
        for (WorldLoopPresets preset : WorldLoopPresets.values()) {
            Button presetButton = presetRow.addChild(Button.builder(presetLabel(preset),
                            button -> this.controls.set(preset.chunkWidth(), preset.netherScale(), preset.endChunkWidth()))
                    .width(PRESET_WIDTH)
                    .build());
            String structuresKey = structureRoomKey(preset.chunkWidth());
            presetButton.setTooltip(Tooltip.create(
                    LoopSizeControls.effectiveLine(preset.chunkWidth()).copy()
                            .append(CommonComponents.NEW_LINE)
                            .append(LoopSizeControls.netherScaleLine(preset.netherScale()))
                            .append(CommonComponents.NEW_LINE)
                            .append(LoopSizeControls.endEffectiveLine(preset.endChunkWidth()))
                            .append(CommonComponents.NEW_LINE)
                            .append(Component.translatable(structuresKey))));
            this.presetButtons.put(preset, presetButton);
        }

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
        Minecraft.getInstance().setScreen(this.parent);
    }

    private static String structureRoomKey(int sizeChunks) {
        if (sizeChunks < VILLAGE_GRID_CHUNKS) {
            return STRUCTURES_SCARCE_KEY;
        }

        if (sizeChunks < MANSION_GRID_CHUNKS) {
            return STRUCTURES_VILLAGES_KEY;
        }

        if (sizeChunks < STRONGHOLD_RING_CHUNKS) {
            return STRUCTURES_COMMON_KEY;
        }

        return STRUCTURES_ALL_KEY;
    }

    private void onControlsChanged() {
        this.doneButton.active = this.controls.isComplete();
        for (Map.Entry<WorldLoopPresets, Button> entry : this.presetButtons.entrySet()) {
            entry.getValue().active = !this.matchesPreset(entry.getKey());
        }
    }

    private boolean matchesPreset(WorldLoopPresets preset) {
        Integer effectiveSize = this.controls.effectiveSize();
        Integer effectiveEndSize = this.controls.effectiveEndSize();
        return effectiveSize != null && effectiveSize == preset.chunkWidth()
                && this.controls.netherScale() == preset.netherScale()
                && effectiveEndSize != null && effectiveEndSize == preset.endChunkWidth();
    }

    private void commit() {
        if (!this.controls.isComplete()) {
            return;
        }

        this.onDone.accept(WorldLoopBounds.ofWidth(this.controls.effectiveSize()), this.controls.netherScale(),
                WorldLoopBounds.ofWidth(this.controls.effectiveEndSize()));
        this.onClose();
    }

    private static Component presetLabel(WorldLoopPresets preset) {
        return Component.translatable(PRESET_KEY_PREFIX + preset.id());
    }

    @FunctionalInterface
    public interface OnDone {
        void accept(WorldLoopBounds wrapping, int netherScale, WorldLoopBounds endWrapping);
    }
}
