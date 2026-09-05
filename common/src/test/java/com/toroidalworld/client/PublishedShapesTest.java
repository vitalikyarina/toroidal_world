package com.toroidalworld.client;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

class PublishedShapesTest {
    private static final WorldFold WIDE = WorldFolds.of(FlatShape.torus(WorldLoopBounds.ofWidth(64)));
    private static final WorldFold NARROW = WorldFolds.of(FlatShape.torus(WorldLoopBounds.ofWidth(16)));
    private static final ResourceKey<Level> DATAPACK_DIMENSION =
            ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath("cject", "hollow"));

    @AfterEach
    void forget() {
        PublishedShapes.clear();
    }

    @Test
    void aDimensionTheServerNeverPublishedAnswersNull() {
        PublishedShapes.publish(Level.OVERWORLD, WIDE);

        assertNull(PublishedShapes.foldOf(Level.NETHER));
        assertNull(PublishedShapes.foldOf(DATAPACK_DIMENSION));
    }

    @Test
    void eachDimensionAnswersItsOwnFold() {
        PublishedShapes.publish(Level.OVERWORLD, WIDE);
        PublishedShapes.publish(Level.NETHER, NARROW);
        PublishedShapes.publish(DATAPACK_DIMENSION, WIDE);

        assertSame(WIDE, PublishedShapes.foldOf(Level.OVERWORLD));
        assertSame(NARROW, PublishedShapes.foldOf(Level.NETHER));
        assertSame(WIDE, PublishedShapes.foldOf(DATAPACK_DIMENSION));
    }

    @Test
    void aRectangleAnswersNullLikeAnUnloopedLevel() {
        PublishedShapes.publish(Level.END, WorldFolds.of(FlatShape.rectangle()));

        assertNull(PublishedShapes.foldOf(Level.END));
    }

    @Test
    void aRepublishReplacesTheEarlierFold() {
        PublishedShapes.publish(Level.NETHER, WIDE);
        PublishedShapes.publish(Level.NETHER, NARROW);

        assertSame(NARROW, PublishedShapes.foldOf(Level.NETHER));
    }

    @Test
    void clearForgetsEveryDimension() {
        PublishedShapes.publish(Level.OVERWORLD, WIDE);
        PublishedShapes.publish(Level.NETHER, NARROW);

        PublishedShapes.clear();

        assertNull(PublishedShapes.foldOf(Level.OVERWORLD));
        assertNull(PublishedShapes.foldOf(Level.NETHER));
    }
}
