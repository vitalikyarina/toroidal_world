package com.toroidalworld.client.screen;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.CoordinateConstants;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopPresets;
import com.toroidalworld.options.WorldLoopSizes;
import com.toroidalworld.options.NetherScales;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.CommonLayouts;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

// What sits behind the Looped shape's Customize button: how wide the world is before it wraps.
//
// The size is counted in chunks because the wrap bounds are chunk bounds — measured in blocks every keystroke would
// have to be rounded to a multiple of 32. Any whole number of chunks is honoured exactly.
//
// The range is where a world still works, so a size outside it holds Done back rather than building something that does
// not — the same as an empty or half-typed field. The field label names the range, so the disabled button has a visible
// reason.
public class WorldLoopSettingsScreen extends Screen {
    private static final Component TITLE = Component.translatable("gui.toroidal_world.toroidal_settings.title");
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

    // Thresholds for the structure-room sentence, read off vanilla 26.2 placement data (StructureSets): the densest
    // common grids — villages, trail ruins, trial chambers — space at 34 chunks (544 blocks); woodland mansions at
    // 80 chunks (1280 blocks); the first stronghold ring reaches out to 168 chunks (2688 blocks) of radius, so
    // doubled it names the width where the ring fits whole.
    private static final int VILLAGE_GRID_CHUNKS = 34;
    private static final int MANSION_GRID_CHUNKS = 80;
    private static final int STRONGHOLD_RING_CHUNKS = 336;

    private static final int FIELD_WIDTH = 310;
    private static final int FIELD_HEIGHT = 20;
    private static final int FIELD_MAX_LENGTH = 7;
    private static final int FOOTER_SPACING = 8;
    private static final int CONTENTS_SPACING = 8;
    private static final int PRESET_SPACING = 5;

    // The row shares the fields' width, so the button width falls out of it rather than being its own choice.
    private static final int PRESET_WIDTH =
            (FIELD_WIDTH - PRESET_SPACING * (WorldLoopPresets.values().length - 1)) / WorldLoopPresets.values().length;

    private final Screen parent;
    private final OnDone onDone;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

    // What the field holds, kept outside the widget: `init()` runs again on every resize and would otherwise rebuild
    // the box from the size the screen opened with, throwing away whatever had been typed.
    private String sizeText;

    // The typed size when it names a world that works — a number within the range — or null otherwise (empty,
    // unparseable, or out of range), which is exactly when Done is held back. Written only by onSizeChanged; everything
    // downstream reads it, so nothing re-parses.
    private @Nullable Integer effectiveSize;

    // The End's twin of sizeText/effectiveSize: its own width with its own floor, coupled to nothing — not the world
    // size, not the nether scale.
    private String endSizeText;
    private @Nullable Integer effectiveEndSize;

    // Kept outside the widget for the same reason, and additionally because it is not free: which scales exist at all
    // depends on the size, so this one is re-checked every time the size changes. Kept normalized to the current size by
    // refreshNetherScale, so commit can hand it over as-is.
    private int netherScale;

    // The scale the player last picked by hand (seeded with the screen's opening scale), as opposed to netherScale,
    // which is what the current width actually allows. The re-pick on a size change seeds from this instead of a flat
    // 1:8, so a hand-picked 1:16 survives large -> huge instead of silently resetting — and a width that disallows it
    // only lowers netherScale, never the wish, so growing back restores the pick.
    private int wantedNetherScale;

    // The width the current scale was picked for. A refresh at a different width re-picks from scratch seeded with
    // wantedNetherScale; a refresh at the same width — init runs again on every resize, and the cycle button re-renders
    // through the same path — keeps the scale the player has, restored or cycled.
    private int scalePickedForSize;

    private EditBox sizeEdit;
    private Button netherScaleButton;
    private EditBox endSizeEdit;
    private Button doneButton;
    private final Map<WorldLoopPresets, Button> presetButtons = new EnumMap<>(WorldLoopPresets.class);

    public WorldLoopSettingsScreen(Screen parent, WorldLoopBounds current, int currentNetherScale,
            WorldLoopBounds currentEnd, OnDone onDone) {
        super(TITLE);
        this.parent = parent;
        this.onDone = onDone;
        this.sizeText = String.valueOf(current.chunkWidth());
        this.netherScale = currentNetherScale;
        this.wantedNetherScale = currentNetherScale;
        this.scalePickedForSize = current.chunkWidth();
        this.endSizeText = String.valueOf(currentEnd.chunkWidth());
    }

    @Override
    protected void init() {
        this.layout.addTitleHeader(TITLE, this.font);

        LinearLayout contents = this.layout.addToContents(LinearLayout.vertical().spacing(CONTENTS_SPACING));

        // A preset is a shortcut into the fields, not a mode: clicking one types the whole configuration it names —
        // overworld width, nether scale, End width — through the same responders a keystroke takes, so the fields stay
        // the single source of truth. The scale is also recorded as the wish, so the next width change re-picks toward
        // the preset's scale rather than toward an older hand pick. The tooltip carries the full configuration plus
        // the structure room — fixed per preset, so the screen itself never changes height over it.
        LinearLayout presetRow = contents.addChild(LinearLayout.horizontal().spacing(PRESET_SPACING));
        for (WorldLoopPresets preset : WorldLoopPresets.values()) {
            Button presetButton = presetRow.addChild(Button.builder(presetLabel(preset),
                            button -> {
                                this.netherScale = preset.netherScale();
                                this.wantedNetherScale = preset.netherScale();
                                this.sizeEdit.setValue(String.valueOf(preset.chunkWidth()));
                                this.endSizeEdit.setValue(String.valueOf(preset.endChunkWidth()));
                            })
                    .width(PRESET_WIDTH)
                    .build());
            String structuresKey = structureRoomKey(preset.chunkWidth());
            presetButton.setTooltip(Tooltip.create(
                    Component.translatable(EFFECTIVE_KEY, preset.chunkWidth(), preset.blockWidth())
                            .append(CommonComponents.NEW_LINE)
                            .append(Component.translatable(NETHER_SCALE_KEY, preset.netherScale()))
                            .append(CommonComponents.NEW_LINE)
                            .append(Component.translatable(END_EFFECTIVE_KEY, preset.endChunkWidth(),
                                    preset.endBlockWidth()))
                            .append(CommonComponents.NEW_LINE)
                            .append(Component.translatable(structuresKey))));
            this.presetButtons.put(preset, presetButton);
        }

        this.sizeEdit = new DigitsEditBox(this.font, FIELD_WIDTH, FIELD_HEIGHT, sizeLabel());
        this.sizeEdit.setMaxLength(FIELD_MAX_LENGTH);
        this.sizeEdit.setValue(this.sizeText);
        this.sizeEdit.setResponder(value -> {
            this.sizeText = value;
            this.onSizeChanged();
        });
        contents.addChild(CommonLayouts.labeledElement(this.font, this.sizeEdit, sizeLabel()));

        this.netherScaleButton = contents.addChild(Button.builder(Component.empty(), button -> this.cycleNetherScale())
                .width(FIELD_WIDTH)
                .build());

        this.endSizeEdit = new DigitsEditBox(this.font, FIELD_WIDTH, FIELD_HEIGHT, endSizeLabel());
        this.endSizeEdit.setMaxLength(FIELD_MAX_LENGTH);
        this.endSizeEdit.setValue(this.endSizeText);
        this.endSizeEdit.setResponder(value -> {
            this.endSizeText = value;
            this.onEndSizeChanged();
        });
        contents.addChild(CommonLayouts.labeledElement(this.font, this.endSizeEdit, endSizeLabel()));

        LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(FOOTER_SPACING));
        this.doneButton = footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> this.commit()).build());
        footer.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose()).build());

        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
        this.onSizeChanged();
        this.onEndSizeChanged();
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(this.parent);
    }

    // An empty or half-typed field names no world at all — that is the only case with nothing to build, and the only
    // one that holds Done back. Every real number names a world, once pulled into the range where one works.
    private void onSizeChanged() {
        Integer sizeChunks = parseSizeChunks(this.sizeEdit.getValue());
        this.effectiveSize = sizeChunks != null && WorldLoopSizes.isInRange(sizeChunks) ? sizeChunks : null;
        this.refreshDoneButton();

        if (this.effectiveSize == null) {
            this.sizeEdit.setTooltip(Tooltip.create(sizeHint(sizeChunks, WorldLoopSizes.MIN_CHUNK_WIDTH, HINT)));
            this.netherScaleButton.active = false;
            this.refreshPresetButtons();
            return;
        }

        this.sizeEdit.setTooltip(Tooltip.create(
                Component.translatable(EFFECTIVE_KEY, this.effectiveSize, this.effectiveSize * CoordinateConstants.CHUNK_WIDTH)
                        .append(CommonComponents.NEW_LINE)
                        .append(HINT)));

        // The scale refresh runs before the highlight: a size change re-picks the scale, and the highlight compares
        // against the picked one.
        this.refreshNetherScale();
        this.refreshPresetButtons();
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

    // The size decides which scales exist, so a change of size re-picks the scale from scratch, seeded with the scale
    // the player last picked by hand (1:8 until they pick one): the wish when the new width allows it, otherwise the
    // largest scale under it. The wish itself never lowers, so a world shrunk under it comes back to it when it grows
    // again — and a player who never touched the button keeps the old 1:8-priority behaviour exactly. A refresh at an
    // unchanged width (resize, the cycle button) keeps the scale the player has. A world with only one usable scale
    // has nothing to cycle through, so the button says so by going inactive. Only reached with a non-null
    // effectiveSize.
    private void refreshNetherScale() {
        int sizeChunks = this.effectiveSize;
        List<Integer> allowed = NetherScales.allowedFor(sizeChunks);
        boolean sizeChanged = sizeChunks != this.scalePickedForSize;
        this.netherScale = NetherScales.normalize(sizeChanged ? this.wantedNetherScale : this.netherScale, allowed);
        this.scalePickedForSize = sizeChunks;
        this.netherScaleButton.active = allowed.size() > 1;

        int netherChunks = NetherScales.netherChunkWidth(sizeChunks, this.netherScale);
        this.netherScaleButton.setMessage(Component.translatable(NETHER_SCALE_KEY, this.netherScale));
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
        this.refreshPresetButtons();
    }

    // The End field mirrors the world size field with its own floor: below it the outer islands — and with them the
    // whole End progression — would not generate. It couples to nothing, so an End change never re-checks the others.
    private void onEndSizeChanged() {
        Integer sizeChunks = parseSizeChunks(this.endSizeEdit.getValue());
        this.effectiveEndSize = sizeChunks != null && WorldLoopSizes.isEndInRange(sizeChunks) ? sizeChunks : null;
        this.refreshDoneButton();
        this.refreshPresetButtons();

        if (this.effectiveEndSize == null) {
            this.endSizeEdit.setTooltip(Tooltip.create(
                    sizeHint(sizeChunks, WorldLoopSizes.END_MIN_CHUNK_WIDTH, END_HINT)));
            return;
        }

        this.endSizeEdit.setTooltip(Tooltip.create(
                Component.translatable(END_EFFECTIVE_KEY, this.effectiveEndSize,
                        this.effectiveEndSize * CoordinateConstants.CHUNK_WIDTH)
                        .append(CommonComponents.NEW_LINE)
                        .append(END_HINT)));
    }

    private void refreshDoneButton() {
        this.doneButton.active = this.effectiveSize != null && this.effectiveEndSize != null;
    }

    // The preset matching the current configuration goes inactive: it reads as "you are here" and a click on it would
    // type nothing new anyway. The match takes all three parameters — width, nether scale, End width — so a
    // hand-cycled scale or an edited End re-arms the button, and clicking it restores the full preset.
    private void refreshPresetButtons() {
        for (Map.Entry<WorldLoopPresets, Button> entry : this.presetButtons.entrySet()) {
            entry.getValue().active = !this.matchesPreset(entry.getKey());
        }
    }

    private boolean matchesPreset(WorldLoopPresets preset) {
        return this.effectiveSize != null && this.effectiveSize == preset.chunkWidth()
                && this.netherScale == preset.netherScale()
                && this.effectiveEndSize != null && this.effectiveEndSize == preset.endChunkWidth();
    }

    private void commit() {
        if (this.effectiveSize == null || this.effectiveEndSize == null) {
            return;
        }

        this.onDone.accept(WorldLoopBounds.ofWidth(this.effectiveSize), this.netherScale,
                WorldLoopBounds.ofWidth(this.effectiveEndSize));
        this.onClose();
    }

    private static Component presetLabel(WorldLoopPresets preset) {
        return Component.translatable(PRESET_KEY_PREFIX + preset.id());
    }

    private static Component sizeLabel() {
        return Component.translatable(SIZE_LABEL_KEY, WorldLoopSizes.MIN_CHUNK_WIDTH, WorldLoopSizes.MAX_CHUNK_WIDTH);
    }

    private static Component endSizeLabel() {
        return Component.translatable(END_SIZE_LABEL_KEY, WorldLoopSizes.END_MIN_CHUNK_WIDTH, WorldLoopSizes.MAX_CHUNK_WIDTH);
    }

    // An empty or half-typed field names no world, so it only gets the general hint; a real number that misses the range
    // says which way it missed and by which bound, then the same hint. The ceiling is the same for every dimension, so
    // only the floor and the hint vary by field.
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

    // Named shape instead of a generic three-arg consumer: two of the values share a type, and only a signature with
    // names says which width is which.
    @FunctionalInterface
    public interface OnDone {
        void accept(WorldLoopBounds wrapping, int netherScale, WorldLoopBounds endWrapping);
    }
}
