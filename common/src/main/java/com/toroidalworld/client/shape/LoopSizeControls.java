package com.toroidalworld.client.shape;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.client.screen.DigitsEditBox;
import com.toroidalworld.core.CoordinateConstants;
import com.toroidalworld.options.NetherScales;
import com.toroidalworld.options.WorldLoopPresets;
import com.toroidalworld.options.WorldLoopSizes;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.CommonLayouts;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class LoopSizeControls {
    private static final Component HINT = Component.translatable("gui.toroidal_world.toroidal_settings.hint");
    private static final String SIZE_LABEL_KEY = "gui.toroidal_world.toroidal_settings.size";
    private static final String EFFECTIVE_KEY = "gui.toroidal_world.toroidal_settings.effective";
    private static final String TOO_SMALL_KEY = "gui.toroidal_world.toroidal_settings.too_small";
    private static final String TOO_LARGE_KEY = "gui.toroidal_world.toroidal_settings.too_large";
    private static final String NETHER_SCALE_KEY = "gui.toroidal_world.toroidal_settings.nether_scale";
    private static final String NETHER_EFFECTIVE_KEY = "gui.toroidal_world.toroidal_settings.nether_effective";
    private static final Component NETHER_HINT = Component.translatable("gui.toroidal_world.toroidal_settings.nether_hint");
    private static final String END_SIZE_LABEL_KEY = "gui.toroidal_world.toroidal_settings.end_size";
    private static final String END_EFFECTIVE_KEY = "gui.toroidal_world.toroidal_settings.end_effective";
    private static final Component END_HINT = Component.translatable("gui.toroidal_world.toroidal_settings.end_hint");

    private static final String PRESET_KEY_PREFIX = "gui.toroidal_world.toroidal_settings.preset.";
    private static final String STRUCTURES_SCARCE_KEY = "gui.toroidal_world.toroidal_settings.consequence.structures_scarce";
    private static final String STRUCTURES_VILLAGES_KEY = "gui.toroidal_world.toroidal_settings.consequence.structures_villages";
    private static final String STRUCTURES_COMMON_KEY = "gui.toroidal_world.toroidal_settings.consequence.structures_common";
    private static final String STRUCTURES_ALL_KEY = "gui.toroidal_world.toroidal_settings.consequence.structures_all";

    private static final int VILLAGE_GRID_CHUNKS = 34;
    private static final int MANSION_GRID_CHUNKS = 80;
    private static final int STRONGHOLD_RING_CHUNKS = 336;

    public static final int FIELD_WIDTH = 310;
    public static final int FIELD_HEIGHT = 20;

    private static final int FIELD_MAX_LENGTH = 7;
    private static final int PRESET_SPACING = 5;

    private static final int PRESET_WIDTH =
            (FIELD_WIDTH - PRESET_SPACING * (WorldLoopPresets.values().length - 1)) / WorldLoopPresets.values().length;

    private final Runnable onChange;

    private String sizeText;
    private @Nullable Integer effectiveSize;

    private String endSizeText;
    private @Nullable Integer effectiveEndSize;

    private int netherScale;
    private int wantedNetherScale;
    private int scalePickedForSize;

    private final Map<WorldLoopPresets, Button> presetButtons = new EnumMap<>(WorldLoopPresets.class);
    private EditBox sizeEdit;
    private Button netherScaleButton;
    private EditBox endSizeEdit;

    public LoopSizeControls(int chunkWidth, int netherScale, int endChunkWidth, Runnable onChange) {
        this.onChange = onChange;
        this.sizeText = String.valueOf(chunkWidth);
        this.netherScale = netherScale;
        this.wantedNetherScale = netherScale;
        this.scalePickedForSize = chunkWidth;
        this.endSizeText = String.valueOf(endChunkWidth);
    }

    public void addPresets(LinearLayout contents) {
        LinearLayout presetRow = contents.addChild(LinearLayout.horizontal().spacing(PRESET_SPACING));
        for (WorldLoopPresets preset : WorldLoopPresets.values()) {
            Button presetButton = presetRow.addChild(Button.builder(presetLabel(preset), button -> this.apply(preset))
                    .width(PRESET_WIDTH)
                    .build());
            presetButton.setTooltip(Tooltip.create(
                    effectiveLine(preset.chunkWidth()).copy()
                            .append(CommonComponents.NEW_LINE)
                            .append(netherScaleLine(preset.netherScale()))
                            .append(CommonComponents.NEW_LINE)
                            .append(endEffectiveLine(preset.endChunkWidth()))
                            .append(CommonComponents.NEW_LINE)
                            .append(Component.translatable(structureRoomKey(preset.chunkWidth())))));
            this.presetButtons.put(preset, presetButton);
        }
    }

    public void addFields(Font font, LinearLayout contents) {
        this.sizeEdit = new DigitsEditBox(font, FIELD_WIDTH, FIELD_HEIGHT, sizeLabel());
        this.sizeEdit.setMaxLength(FIELD_MAX_LENGTH);
        this.sizeEdit.setValue(this.sizeText);
        this.sizeEdit.setResponder(value -> {
            this.sizeText = value;
            this.onSizeChanged();
        });
        contents.addChild(CommonLayouts.labeledElement(font, this.sizeEdit, sizeLabel()));

        this.netherScaleButton = contents.addChild(Button.builder(Component.empty(), button -> this.cycleNetherScale())
                .width(FIELD_WIDTH)
                .build());

        this.endSizeEdit = new DigitsEditBox(font, FIELD_WIDTH, FIELD_HEIGHT, endSizeLabel());
        this.endSizeEdit.setMaxLength(FIELD_MAX_LENGTH);
        this.endSizeEdit.setValue(this.endSizeText);
        this.endSizeEdit.setResponder(value -> {
            this.endSizeText = value;
            this.onEndSizeChanged();
        });
        contents.addChild(CommonLayouts.labeledElement(font, this.endSizeEdit, endSizeLabel()));
    }

    public void refresh() {
        this.onSizeChanged();
        this.onEndSizeChanged();
    }

    public @Nullable Integer effectiveSize() {
        return this.effectiveSize;
    }

    public int netherScale() {
        return this.netherScale;
    }

    public @Nullable Integer effectiveEndSize() {
        return this.effectiveEndSize;
    }

    public boolean isComplete() {
        return this.effectiveSize != null && this.effectiveEndSize != null;
    }

    private void apply(WorldLoopPresets preset) {
        this.netherScale = preset.netherScale();
        this.wantedNetherScale = preset.netherScale();
        this.sizeEdit.setValue(String.valueOf(preset.chunkWidth()));
        this.endSizeEdit.setValue(String.valueOf(preset.endChunkWidth()));
    }

    private boolean matchesPreset(WorldLoopPresets preset) {
        return this.effectiveSize != null && this.effectiveSize == preset.chunkWidth()
                && this.netherScale == preset.netherScale()
                && this.effectiveEndSize != null && this.effectiveEndSize == preset.endChunkWidth();
    }

    private void changed() {
        for (Map.Entry<WorldLoopPresets, Button> entry : this.presetButtons.entrySet()) {
            entry.getValue().active = !this.matchesPreset(entry.getKey());
        }

        this.onChange.run();
    }

    private void onSizeChanged() {
        Integer sizeChunks = parseSizeChunks(this.sizeEdit.getValue());
        this.effectiveSize = sizeChunks != null && WorldLoopSizes.isInRange(sizeChunks) ? sizeChunks : null;

        if (this.effectiveSize == null) {
            this.sizeEdit.setTooltip(Tooltip.create(sizeHint(sizeChunks, WorldLoopSizes.MIN_CHUNK_WIDTH, HINT)));
            this.netherScaleButton.active = false;
            this.changed();
            return;
        }

        this.sizeEdit.setTooltip(Tooltip.create(
                effectiveLine(this.effectiveSize).copy().append(CommonComponents.NEW_LINE).append(HINT)));

        this.refreshNetherScale();
        this.changed();
    }

    private void refreshNetherScale() {
        int sizeChunks = this.effectiveSize;
        List<Integer> allowed = NetherScales.allowedFor(sizeChunks);
        boolean sizeChanged = sizeChunks != this.scalePickedForSize;
        this.netherScale = NetherScales.normalize(sizeChanged ? this.wantedNetherScale : this.netherScale, allowed);
        this.scalePickedForSize = sizeChunks;
        this.netherScaleButton.active = allowed.size() > 1;

        int netherChunks = NetherScales.netherChunkWidth(sizeChunks, this.netherScale);
        this.netherScaleButton.setMessage(netherScaleLine(this.netherScale));
        this.netherScaleButton.setTooltip(Tooltip.create(
                Component.translatable(NETHER_EFFECTIVE_KEY, netherChunks, netherChunks * CoordinateConstants.CHUNK_WIDTH)
                        .append(CommonComponents.NEW_LINE)
                        .append(NETHER_HINT)));
    }

    private void cycleNetherScale() {
        if (this.effectiveSize == null) {
            return;
        }

        this.netherScale = NetherScales.next(this.netherScale, this.effectiveSize);
        this.wantedNetherScale = this.netherScale;
        this.refreshNetherScale();
        this.changed();
    }

    private void onEndSizeChanged() {
        Integer sizeChunks = parseSizeChunks(this.endSizeEdit.getValue());
        this.effectiveEndSize = sizeChunks != null && WorldLoopSizes.isEndInRange(sizeChunks) ? sizeChunks : null;

        if (this.effectiveEndSize == null) {
            this.endSizeEdit.setTooltip(Tooltip.create(
                    sizeHint(sizeChunks, WorldLoopSizes.END_MIN_CHUNK_WIDTH, END_HINT)));
            this.changed();
            return;
        }

        this.endSizeEdit.setTooltip(Tooltip.create(
                endEffectiveLine(this.effectiveEndSize).copy().append(CommonComponents.NEW_LINE).append(END_HINT)));
        this.changed();
    }

    private static Component presetLabel(WorldLoopPresets preset) {
        return Component.translatable(PRESET_KEY_PREFIX + preset.id());
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

    private static Component effectiveLine(int chunkWidth) {
        return Component.translatable(EFFECTIVE_KEY, chunkWidth, chunkWidth * CoordinateConstants.CHUNK_WIDTH);
    }

    private static Component netherScaleLine(int scale) {
        return Component.translatable(NETHER_SCALE_KEY, scale);
    }

    private static Component endEffectiveLine(int endChunkWidth) {
        return Component.translatable(END_EFFECTIVE_KEY, endChunkWidth, endChunkWidth * CoordinateConstants.CHUNK_WIDTH);
    }

    private static Component sizeLabel() {
        return Component.translatable(SIZE_LABEL_KEY, WorldLoopSizes.MIN_CHUNK_WIDTH, WorldLoopSizes.MAX_CHUNK_WIDTH);
    }

    private static Component endSizeLabel() {
        return Component.translatable(END_SIZE_LABEL_KEY, WorldLoopSizes.END_MIN_CHUNK_WIDTH, WorldLoopSizes.MAX_CHUNK_WIDTH);
    }

    private static Component sizeHint(@Nullable Integer sizeChunks, int minChunks, Component hint) {
        if (sizeChunks == null) {
            return hint;
        }

        Component bound = sizeChunks < minChunks
                ? Component.translatable(TOO_SMALL_KEY, minChunks)
                : Component.translatable(TOO_LARGE_KEY, WorldLoopSizes.MAX_CHUNK_WIDTH);
        return bound.copy().append(CommonComponents.NEW_LINE).append(hint);
    }

    private static @Nullable Integer parseSizeChunks(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
