package com.toroidalworld.client.shape.torus;

import com.toroidalworld.api.v1.client.WorldOptionContext;
import com.toroidalworld.api.v1.client.WorldOptionControl;
import com.toroidalworld.client.shape.LoopSizeControls;
import com.toroidalworld.api.v1.option.GenerationOptions;
import com.toroidalworld.shape.torus.GuaranteedLand;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.Component;

public final class GuaranteedLandControl implements WorldOptionControl {
    private static final Component LABEL =
            Component.translatable("gui.toroidal_world.toroidal_settings.guaranteed_land");
    private static final Component HINT =
            Component.translatable("gui.toroidal_world.toroidal_settings.guaranteed_land_hint");

    private boolean guaranteedLand;

    public GuaranteedLandControl(WorldOptionContext context) {
        this.guaranteedLand = context.options().get(GuaranteedLand.OPTION);
    }

    @Override
    public void addWidgets(Font font, LinearLayout contents) {
        contents.addChild(CycleButton.onOffBuilder(this.guaranteedLand)
                .withTooltip(chosen -> Tooltip.create(HINT))
                .create(0, 0, LoopSizeControls.FIELD_WIDTH, LoopSizeControls.FIELD_HEIGHT, LABEL,
                        (button, chosen) -> this.guaranteedLand = chosen));
    }

    @Override
    public GenerationOptions commit(GenerationOptions options) {
        return options.with(GuaranteedLand.OPTION, this.guaranteedLand);
    }
}
