package com.toroidalworld.client.shape;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntPredicate;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.CoordinateConstants;
import com.toroidalworld.core.NetherScales;
import com.toroidalworld.core.WorldLoopSizes;
import com.toroidalworld.shape.WorldLoopPresets;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
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

    private final SizeField size;
    private final SizeField endSize;

    private int netherScale;
    private int wantedNetherScale;
    private int scalePickedForSize;

    private final Map<WorldLoopPresets, Button> presetButtons = new EnumMap<>(WorldLoopPresets.class);
    private Button netherScaleButton;

    public LoopSizeControls(int chunkWidth, int netherScale, int endChunkWidth, Runnable onChange) {
        this.onChange = onChange;
        this.size = new SizeField(SIZE_LABEL_KEY, EFFECTIVE_KEY, HINT,
                WorldLoopSizes.MIN_CHUNK_WIDTH, WorldLoopSizes::isInRange, chunkWidth);
        this.endSize = new SizeField(END_SIZE_LABEL_KEY, END_EFFECTIVE_KEY, END_HINT,
                WorldLoopSizes.END_MIN_CHUNK_WIDTH, WorldLoopSizes::isEndInRange, endChunkWidth);
        this.netherScale = netherScale;
        this.wantedNetherScale = netherScale;
        this.scalePickedForSize = chunkWidth;
    }

    public void addPresets(LinearLayout contents) {
        LinearLayout presetRow = contents.addChild(LinearLayout.horizontal().spacing(PRESET_SPACING));
        for (WorldLoopPresets preset : WorldLoopPresets.values()) {
            Button presetButton = presetRow.addChild(Button.builder(presetLabel(preset), button -> this.apply(preset))
                    .width(PRESET_WIDTH)
                    .build());
            presetButton.setTooltip(Tooltip.create(
                    this.size.effectiveLine(preset.chunkWidth()).copy()
                            .append(CommonComponents.NEW_LINE)
                            .append(netherScaleLine(preset.netherScale()))
                            .append(CommonComponents.NEW_LINE)
                            .append(this.endSize.effectiveLine(preset.endChunkWidth()))
                            .append(CommonComponents.NEW_LINE)
                            .append(Component.translatable(structureRoomKey(preset.chunkWidth())))));
            this.presetButtons.put(preset, presetButton);
        }
    }

    public void addFields(Font font, LinearLayout contents) {
        this.size.add(font, contents, this::onSizeChanged);

        this.netherScaleButton = contents.addChild(Button.builder(Component.empty(), button -> this.cycleNetherScale())
                .width(FIELD_WIDTH)
                .build());

        this.endSize.add(font, contents, this::onEndSizeChanged);
    }

    public void refresh() {
        this.onSizeChanged();
        this.onEndSizeChanged();
    }

    public @Nullable Integer effectiveSize() {
        return this.size.effective();
    }

    public int netherScale() {
        return this.netherScale;
    }

    public @Nullable Integer effectiveEndSize() {
        return this.endSize.effective();
    }

    public boolean isComplete() {
        return this.size.effective() != null && this.endSize.effective() != null;
    }

    private void apply(WorldLoopPresets preset) {
        this.netherScale = preset.netherScale();
        this.wantedNetherScale = preset.netherScale();
        this.size.setValue(preset.chunkWidth());
        this.endSize.setValue(preset.endChunkWidth());
    }

    private boolean matchesPreset(WorldLoopPresets preset) {
        Integer effectiveSize = this.size.effective();
        Integer effectiveEndSize = this.endSize.effective();
        return effectiveSize != null && effectiveSize == preset.chunkWidth()
                && this.netherScale == preset.netherScale()
                && effectiveEndSize != null && effectiveEndSize == preset.endChunkWidth();
    }

    private void changed() {
        for (Map.Entry<WorldLoopPresets, Button> entry : this.presetButtons.entrySet()) {
            entry.getValue().active = !this.matchesPreset(entry.getKey());
        }

        this.onChange.run();
    }

    private void onSizeChanged() {
        this.size.update();

        if (this.size.effective() == null) {
            this.netherScaleButton.active = false;
        } else {
            this.refreshNetherScale();
        }

        this.changed();
    }

    private void onEndSizeChanged() {
        this.endSize.update();
        this.changed();
    }

    private void refreshNetherScale() {
        int sizeChunks = this.size.effective();
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
        Integer effectiveSize = this.size.effective();
        if (effectiveSize == null) {
            return;
        }

        this.netherScale = NetherScales.next(this.netherScale, effectiveSize);
        this.wantedNetherScale = this.netherScale;
        this.refreshNetherScale();
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

    private static Component netherScaleLine(int scale) {
        return Component.translatable(NETHER_SCALE_KEY, scale);
    }

    private static final class SizeField {
        private final String labelKey;
        private final String effectiveKey;
        private final Component hint;
        private final int minChunks;
        private final IntPredicate inRange;

        private String text;
        private @Nullable Integer effective;
        private DigitsEditBox edit;

        private SizeField(String labelKey, String effectiveKey, Component hint, int minChunks, IntPredicate inRange,
                int chunkWidth) {
            this.labelKey = labelKey;
            this.effectiveKey = effectiveKey;
            this.hint = hint;
            this.minChunks = minChunks;
            this.inRange = inRange;
            this.text = String.valueOf(chunkWidth);
        }

        private void add(Font font, LinearLayout contents, Runnable onEdited) {
            this.edit = new DigitsEditBox(font, FIELD_WIDTH, FIELD_HEIGHT, this.label());
            this.edit.setMaxLength(FIELD_MAX_LENGTH);
            this.edit.setValue(this.text);
            this.edit.setResponder(value -> {
                this.text = value;
                onEdited.run();
            });
            contents.addChild(CommonLayouts.labeledElement(font, this.edit, this.label()));
        }

        private void update() {
            Integer sizeChunks = this.edit.number();
            this.effective = sizeChunks != null && this.inRange.test(sizeChunks) ? sizeChunks : null;
            this.edit.setTooltip(Tooltip.create(this.effective != null
                    ? this.effectiveLine(this.effective).copy().append(CommonComponents.NEW_LINE).append(this.hint)
                    : this.boundHint(sizeChunks)));
        }

        private void setValue(int chunkWidth) {
            this.edit.setValue(String.valueOf(chunkWidth));
        }

        private @Nullable Integer effective() {
            return this.effective;
        }

        private Component effectiveLine(int chunkWidth) {
            return Component.translatable(this.effectiveKey, chunkWidth, chunkWidth * CoordinateConstants.CHUNK_WIDTH);
        }

        private Component label() {
            return Component.translatable(this.labelKey, this.minChunks, WorldLoopSizes.MAX_CHUNK_WIDTH);
        }

        private Component boundHint(@Nullable Integer sizeChunks) {
            if (sizeChunks == null) {
                return this.hint;
            }

            Component bound = sizeChunks < this.minChunks
                    ? Component.translatable(TOO_SMALL_KEY, this.minChunks)
                    : Component.translatable(TOO_LARGE_KEY, WorldLoopSizes.MAX_CHUNK_WIDTH);
            return bound.copy().append(CommonComponents.NEW_LINE).append(this.hint);
        }
    }
}
