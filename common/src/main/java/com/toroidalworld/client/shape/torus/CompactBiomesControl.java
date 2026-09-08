package com.toroidalworld.client.shape.torus;

import java.util.Locale;
import java.util.OptionalDouble;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.client.options.WorldOptionContext;
import com.toroidalworld.client.options.WorldOptionControl;
import com.toroidalworld.client.screen.DigitsEditBox;
import com.toroidalworld.client.shape.LoopSizeControls;
import com.toroidalworld.core.GenerationOptions;
import com.toroidalworld.shape.torus.ClimateScale;
import com.toroidalworld.shape.torus.CompactBiomes;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.CommonLayouts;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class CompactBiomesControl implements WorldOptionControl {
    private static final Component LABEL =
            Component.translatable("gui.toroidal_world.toroidal_settings.climate_compression");
    private static final Component KEEPS =
            Component.translatable("gui.toroidal_world.toroidal_settings.climate_keeps");
    private static final Component FACTOR_LABEL =
            Component.translatable("gui.toroidal_world.toroidal_settings.climate_factor");
    private static final Component FACTOR_HINT =
            Component.translatable("gui.toroidal_world.toroidal_settings.climate_factor_hint");

    private static final String MODE_KEY_PREFIX = "gui.toroidal_world.toroidal_settings.climate_mode.";
    private static final String MODE_HINT_SUFFIX = ".hint";

    private static final int FACTOR_MAX_LENGTH = 5;
    private static final String UNKNOWN_FACTOR = "—";
    private static final String FRACTIONAL_FACTOR_FORMAT = "%.2f";

    private final WorldOptionContext context;

    private ClimateScale climateScale;
    private String factorText;
    private @Nullable Integer effectiveFactor;
    private EditBox factorEdit;

    public CompactBiomesControl(WorldOptionContext context) {
        this.context = context;
        this.climateScale = context.options().get(CompactBiomes.OPTION);
        this.factorText = String.valueOf(this.climateScale.factor());
        this.effectiveFactor = this.climateScale.factor();
    }

    @Override
    public void addWidgets(Font font, LinearLayout contents) {
        contents.addChild(CycleButton.builder(CompactBiomesControl::modeLabel, this.climateScale.mode())
                .withValues(ClimateScale.Mode.values())
                .withTooltip(mode -> Tooltip.create(modeHint(mode)))
                .create(0, 0, LoopSizeControls.FIELD_WIDTH, LoopSizeControls.FIELD_HEIGHT, LABEL,
                        (button, mode) -> this.chooseMode(mode)));

        contents.addChild(CommonLayouts.labeledElement(font, this.factorField(font), FACTOR_LABEL));
    }

    @Override
    public void onSharedStateChanged() {
        if (this.climateScale.mode() != ClimateScale.Mode.CUSTOM) {
            this.factorEdit.setValue(this.previewedFactor());
        }
    }

    @Override
    public boolean isComplete() {
        return this.climateScale.mode() != ClimateScale.Mode.CUSTOM || this.effectiveFactor != null;
    }

    @Override
    public GenerationOptions commit(GenerationOptions options) {
        ClimateScale chosen = this.climateScale.mode() == ClimateScale.Mode.CUSTOM
                ? ClimateScale.custom(this.effectiveFactor)
                : this.climateScale;
        return options.with(CompactBiomes.OPTION, chosen);
    }

    private EditBox factorField(Font font) {
        boolean custom = this.climateScale.mode() == ClimateScale.Mode.CUSTOM;
        this.factorEdit = new DigitsEditBox(font, LoopSizeControls.FIELD_WIDTH,
                LoopSizeControls.FIELD_HEIGHT, FACTOR_LABEL);
        this.factorEdit.setMaxLength(FACTOR_MAX_LENGTH);
        this.factorEdit.setTooltip(Tooltip.create(FACTOR_HINT));
        this.factorEdit.setEditable(custom);
        this.factorEdit.setValue(custom ? this.factorText : this.previewedFactor());
        if (custom) {
            this.factorEdit.setResponder(value -> {
                this.factorText = value;
                this.effectiveFactor = parseFactor(value);
                this.context.onChanged();
            });
        }

        return this.factorEdit;
    }

    private String previewedFactor() {
        Integer chunkWidth = this.context.loopChunkWidth();
        if (chunkWidth == null) {
            return UNKNOWN_FACTOR;
        }

        OptionalDouble factor = ClimateFactorPreview.temperatureFactor(this.context.parent(),
                this.context.options().with(CompactBiomes.OPTION, this.climateScale), chunkWidth);
        return factor.isPresent() ? display(factor.getAsDouble()) : UNKNOWN_FACTOR;
    }

    private void chooseMode(ClimateScale.Mode mode) {
        this.climateScale = this.climateScale.withMode(mode);
        this.context.rebuild();
    }

    private static String display(double factor) {
        return factor == Math.rint(factor)
                ? Integer.toString((int) factor)
                : String.format(Locale.ROOT, FRACTIONAL_FACTOR_FORMAT, factor);
    }

    private static Component modeLabel(ClimateScale.Mode mode) {
        return mode == ClimateScale.Mode.OFF
                ? CommonComponents.OPTION_OFF
                : Component.translatable(MODE_KEY_PREFIX + mode.getSerializedName());
    }

    private static Component modeHint(ClimateScale.Mode mode) {
        Component hint = Component.translatable(
                MODE_KEY_PREFIX + mode.getSerializedName() + MODE_HINT_SUFFIX);
        return mode == ClimateScale.Mode.OFF
                ? hint
                : hint.copy().append(CommonComponents.NEW_LINE).append(KEEPS);
    }

    private static @Nullable Integer parseFactor(String value) {
        try {
            int factor = Integer.parseInt(value);
            return factor >= ClimateScale.CUSTOM_MIN && factor <= ClimateScale.CUSTOM_MAX ? factor : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
