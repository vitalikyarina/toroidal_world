package com.toroidalworld.compat.sable;

import java.util.Optional;

import org.joml.Vector2i;

import com.toroidalworld.core.ForeignFrame;
import com.toroidalworld.core.ForeignFrameSource;
import com.toroidalworld.core.ForeignSpan;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;

import net.minecraft.world.level.Level;

final class SableFrames implements ForeignFrameSource {
    @Override
    public Optional<ForeignFrame> frameOf(Level level) {
        SubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) {
            return Optional.empty();
        }

        Vector2i origin = container.getOrigin();
        int logPlotSize = container.getLogPlotSize();
        int plotsPerSide = 1 << container.getLogSideLength();
        return Optional.of(new ForeignFrame(
                plotChunks(origin.x, plotsPerSide, logPlotSize),
                plotChunks(origin.y, plotsPerSide, logPlotSize)));
    }

    private static ForeignSpan plotChunks(int originPlot, int plotsPerSide, int logPlotSize) {
        return new ForeignSpan(originPlot << logPlotSize, (originPlot + plotsPerSide) << logPlotSize);
    }
}
