package com.toroidalworld.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.world.phys.Vec3;

class DimensionMappingTest {
    private static final double DECLARED = 0.125;

    private static WorldFold xOnly(int minChunk, int maxChunk) {
        return WorldFolds.of(FlatShape.cylinder(
                new WorldLoopBounds(new AxisBounds.Looped(minChunk, maxChunk), AxisBounds.Unbounded.INSTANCE)));
    }

    @Test
    void aWorldLoopedInOneAxisScalesThatAxisAndDeclaresTheOther() {
        Vec3 mapped = DimensionMapping.map(xOnly(-32, 32), xOnly(-4, 4), new Vec3(256.0, 70.0, 800.0), DECLARED);

        assertEquals(256.0 * (8 * 16) / (64 * 16), mapped.x);
        assertEquals(70.0, mapped.y);
        assertEquals(800.0 * DECLARED, mapped.z);
    }

    @Test
    void anUnwrappedWorldMapsByTheDeclaredScaleAlone() {
        Vec3 mapped = DimensionMapping.map(WorldFolds.NOOP, WorldFolds.NOOP, new Vec3(80.0, 70.0, -160.0), DECLARED);

        assertEquals(80.0 * DECLARED, mapped.x);
        assertEquals(70.0, mapped.y);
        assertEquals(-160.0 * DECLARED, mapped.z);
    }

    @Test
    void aPositionThatMapsToItselfComesBackUntouched() {
        WorldFold world = xOnly(-32, 32);
        Vec3 position = new Vec3(100.0, 70.0, -3000.0);

        assertSame(position, DimensionMapping.map(world, world, position, 1.0));
    }

    @Test
    void aShapeThatDoesNotDecomposeHasNoMappingYet() {
        WorldFold skewed = new DeckGroupFold(FlatShape.latticeTorus(WorldLoopBounds.ofWidth(64), 5));

        assertThrows(IllegalStateException.class,
                () -> DimensionMapping.map(skewed, xOnly(-4, 4), new Vec3(1.0, 70.0, 1.0), DECLARED));
    }
}
