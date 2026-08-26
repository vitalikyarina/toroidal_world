package com.toroidalworld.compat.journeymap;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2fStack;

public interface JourneyMapSeamPass {
    void toroidal$drawSeams(GuiGraphicsExtractor graphics, Matrix3x2fStack pose, double offsetX, double offsetZ);
}
