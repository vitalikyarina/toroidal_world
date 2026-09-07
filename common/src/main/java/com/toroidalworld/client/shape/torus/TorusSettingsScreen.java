package com.toroidalworld.client.shape.torus;

import java.util.Locale;
import java.util.OptionalDouble;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.client.screen.DigitsEditBox;
import com.toroidalworld.client.shape.LoopSizeControls;
import com.toroidalworld.options.ClimateScale;
import com.toroidalworld.options.WorldLoopBounds;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.CommonLayouts;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class TorusSettingsScreen extends Screen {
    private static final Component TITLE = Component.translatable("gui.toroidal_world.toroidal_settings.title");
    private static final Component CLIMATE_LABEL =
            Component.translatable("gui.toroidal_world.toroidal_settings.climate_compression");
    private static final Component ADDITIONAL_SECTION =
            Component.translatable("gui.toroidal_world.toroidal_settings.section.additional")
                    .withStyle(ChatFormatting.BOLD);
    private static final Component CLIMATE_KEEPS =
            Component.translatable("gui.toroidal_world.toroidal_settings.climate_keeps");
    private static final Component CLIMATE_FACTOR_HINT =
            Component.translatable("gui.toroidal_world.toroidal_settings.climate_factor_hint");

    private static final String CLIMATE_MODE_KEY_PREFIX = "gui.toroidal_world.toroidal_settings.climate_mode.";
    private static final String CLIMATE_MODE_HINT_SUFFIX = ".hint";
    private static final Component FACTOR_LABEL =
            Component.translatable("gui.toroidal_world.toroidal_settings.climate_factor");

    private static final int FACTOR_MAX_LENGTH = 5;
    private static final String UNKNOWN_FACTOR = "—";
    private static final String FRACTIONAL_FACTOR_FORMAT = "%.2f";
    private static final int FOOTER_SPACING = 8;
    private static final int CONTENTS_SPACING = 8;

    private final Screen parent;
    private final OnDone onDone;
    private final LoopSizeControls controls;

    private HeaderAndFooterLayout layout;

    private ClimateScale climateScale;
    private String factorText;
    private @Nullable Integer effectiveFactor;
    private EditBox factorEdit;
    private Button doneButton;

    public TorusSettingsScreen(Screen parent, WorldLoopBounds current, int currentNetherScale,
            WorldLoopBounds currentEnd, ClimateScale currentClimateScale, OnDone onDone) {
        super(TITLE);
        this.parent = parent;
        this.onDone = onDone;
        this.climateScale = currentClimateScale;
        this.factorText = String.valueOf(currentClimateScale.factor());
        this.effectiveFactor = currentClimateScale.factor();
        this.controls = new LoopSizeControls(current.chunkWidth(), currentNetherScale, currentEnd.chunkWidth(),
                this::onControlsChanged);
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

        contents.addChild(CycleButton.builder(TorusSettingsScreen::modeLabel)
                .withValues(ClimateScale.Mode.values())
                .withInitialValue(this.climateScale.mode())
                .withTooltip(mode -> Tooltip.create(modeHint(mode)))
                .create(0, 0, LoopSizeControls.FIELD_WIDTH, LoopSizeControls.FIELD_HEIGHT, CLIMATE_LABEL,
                        (button, mode) -> this.chooseMode(mode)));

        contents.addChild(CommonLayouts.labeledElement(this.font, this.factorField(), FACTOR_LABEL));

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

    private EditBox factorField() {
        boolean custom = this.climateScale.mode() == ClimateScale.Mode.CUSTOM;
        this.factorEdit = new DigitsEditBox(this.font, LoopSizeControls.FIELD_WIDTH,
                LoopSizeControls.FIELD_HEIGHT, FACTOR_LABEL);
        this.factorEdit.setMaxLength(FACTOR_MAX_LENGTH);
        this.factorEdit.setTooltip(Tooltip.create(CLIMATE_FACTOR_HINT));
        this.factorEdit.setEditable(custom);
        this.factorEdit.setValue(custom ? this.factorText : previewedFactor());
        if (custom) {
            this.factorEdit.setResponder(value -> {
                this.factorText = value;
                this.effectiveFactor = parseFactor(value);
                this.refreshDoneButton();
            });
        }

        return this.factorEdit;
    }

    private String previewedFactor() {
        Integer chunkWidth = this.controls.effectiveSize();
        if (chunkWidth == null) {
            return UNKNOWN_FACTOR;
        }

        OptionalDouble factor = ClimateFactorPreview.temperatureFactor(this.parent, this.climateScale, chunkWidth);
        return factor.isPresent() ? display(factor.getAsDouble()) : UNKNOWN_FACTOR;
    }

    private static String display(double factor) {
        return factor == Math.rint(factor)
                ? Integer.toString((int) factor)
                : String.format(Locale.ROOT, FRACTIONAL_FACTOR_FORMAT, factor);
    }

    private void onControlsChanged() {
        this.refreshDoneButton();
        if (this.climateScale.mode() != ClimateScale.Mode.CUSTOM) {
            this.factorEdit.setValue(previewedFactor());
        }
    }

    private void chooseMode(ClimateScale.Mode mode) {
        this.climateScale = this.climateScale.withMode(mode);
        this.rebuildWidgets();
    }

    private boolean isComplete() {
        return this.controls.isComplete()
                && (this.climateScale.mode() != ClimateScale.Mode.CUSTOM || this.effectiveFactor != null);
    }

    private void refreshDoneButton() {
        this.doneButton.active = this.isComplete();
    }

    private void commit() {
        if (!this.isComplete()) {
            return;
        }

        ClimateScale chosenClimateScale = this.climateScale.mode() == ClimateScale.Mode.CUSTOM
                ? ClimateScale.custom(this.effectiveFactor)
                : this.climateScale;
        this.onDone.accept(WorldLoopBounds.ofWidth(this.controls.effectiveSize()), this.controls.netherScale(),
                WorldLoopBounds.ofWidth(this.controls.effectiveEndSize()), chosenClimateScale);
        this.onClose();
    }

    private static Component modeLabel(ClimateScale.Mode mode) {
        return Component.translatable(CLIMATE_MODE_KEY_PREFIX + mode.getSerializedName());
    }

    private static Component modeHint(ClimateScale.Mode mode) {
        Component hint = Component.translatable(
                CLIMATE_MODE_KEY_PREFIX + mode.getSerializedName() + CLIMATE_MODE_HINT_SUFFIX);
        return mode == ClimateScale.Mode.OFF
                ? hint
                : hint.copy().append(CommonComponents.NEW_LINE).append(CLIMATE_KEEPS);
    }

    private static @Nullable Integer parseFactor(String value) {
        try {
            int factor = Integer.parseInt(value);
            return factor >= ClimateScale.CUSTOM_MIN && factor <= ClimateScale.CUSTOM_MAX ? factor : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @FunctionalInterface
    public interface OnDone {
        void accept(WorldLoopBounds wrapping, int netherScale, WorldLoopBounds endWrapping,
                ClimateScale climateScale);
    }
}
