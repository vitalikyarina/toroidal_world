package com.toroidalworld.api.v1.shape;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.api.v1.option.GenerationOptions;
import com.toroidalworld.core.CarriedShape;
import com.toroidalworld.core.CoordinateConstants;
import com.toroidalworld.core.FlatShape;
import com.toroidalworld.core.WorldLoopBounds;
import com.toroidalworld.engine.gen.ShapedDimensions;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;

/**
 * The one door a shape has into the world's dimensions: it writes the declared spans into the generators of the three
 * vanilla stems, and reads them back out of an existing world. Everything a folding world then does — the wrapped
 * noise, the packet rewriting, the seam behaviour — is derived from what is written here and is not reachable from
 * this package.
 *
 * <p>Call {@link #withSpans} from {@link ShapeModule.Apply} and {@link #spansOf} from {@link ShapeModule.Read}. The
 * dimensions handed to {@code Apply} have had any earlier shape stripped, so a shape states its whole geometry rather
 * than amending someone else's.</p>
 */
public final class ShapeDimensions {

    /**
     * The dimensions with each of the three vanilla stems carrying its declared spans and the world's options. A stem
     * whose generator cannot take a shape is left alone, so a datapack dimension of another mod is never rewritten.
     *
     * @throws IllegalArgumentException if a declared shape is one the engine cannot fold — today, anything whose axes
     *         do not fold independently of one another
     */
    public static WorldDimensions withSpans(WorldDimensions dimensions, LoopSpans overworld, LoopSpans nether,
            LoopSpans end, GenerationOptions options) {
        return ShapedDimensions.withShapes(dimensions,
                carried(overworld, options),
                carried(nether, options),
                carried(end, options));
    }

    /**
     * The spans that stem was created with, or {@code null} where it carries no shape of ours. A shape reads this in
     * {@link ShapeModule.Read} and refuses anything that is not its own geometry — normally by checking which axes
     * {@link LoopSpans#loops} answers for.
     */
    public static @Nullable LoopSpans spansOf(WorldDimensions dimensions, ResourceKey<LevelStem> key) {
        FlatShape shape = ShapedDimensions.shapeOf(dimensions, key);
        return shape == null || !shape.decomposesPerAxis() ? null : spans(shape.bounds());
    }

    /**
     * The world options that stem was created with, or {@link GenerationOptions#DEFAULT} where it carries no shape of
     * ours. Options are stored per stem but written once for the whole world, so the overworld's are the world's.
     */
    public static GenerationOptions optionsOf(WorldDimensions dimensions, ResourceKey<LevelStem> key) {
        CarriedShape carried = ShapedDimensions.carriedShapeOf(dimensions, key);
        return carried == null ? GenerationOptions.DEFAULT : carried.generationOptions();
    }

    private static CarriedShape carried(LoopSpans spans, GenerationOptions options) {
        return new CarriedShape(new FlatShape(bounds(spans), FlatShape.NO_SKEW, null), options);
    }

    private static WorldLoopBounds bounds(LoopSpans spans) {
        WorldLoopBounds bounds = WorldLoopBounds.UNBOUNDED;
        for (Direction.Axis axis : CoordinateConstants.HORIZONTAL_AXES) {
            if (spans.loops(axis)) {
                bounds = withAxis(bounds, axis, spans.minChunk(axis), spans.maxChunk(axis));
            }
        }

        return bounds;
    }

    private static WorldLoopBounds withAxis(WorldLoopBounds bounds, Direction.Axis axis, int minChunk, int maxChunk) {
        WorldLoopBounds.AxisBounds looped = new WorldLoopBounds.AxisBounds.Looped(minChunk, maxChunk);
        return axis == Direction.Axis.X
                ? new WorldLoopBounds(looped, bounds.z())
                : new WorldLoopBounds(bounds.x(), looped);
    }

    private static LoopSpans spans(WorldLoopBounds bounds) {
        LoopSpans spans = LoopSpans.NONE;
        for (Direction.Axis axis : CoordinateConstants.HORIZONTAL_AXES) {
            if (bounds.axis(axis) instanceof WorldLoopBounds.AxisBounds.Looped looped) {
                spans = spans.and(axis, looped.minChunk(), looped.maxChunk());
            }
        }

        return spans;
    }

    private ShapeDimensions() {
    }
}
