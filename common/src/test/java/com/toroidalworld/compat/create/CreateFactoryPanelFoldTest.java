package com.toroidalworld.compat.create;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelSlot;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import com.toroidalworld.core.DeckGroupFold;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

class CreateFactoryPanelFoldTest {
    private static final int WORLD_CHUNKS = 16;
    private static final int WORLD_BLOCKS = WORLD_CHUNKS * 2 * 16;
    private static final int SKEW_CHUNKS = 4;
    private static final int MIRROR_LINE_CHUNK = 3;

    private static final WorldLoopBounds BOUNDS =
            new WorldLoopBounds(-WORLD_CHUNKS, WORLD_CHUNKS, -WORLD_CHUNKS, WORLD_CHUNKS);
    private static final WorldLoopBounds X_ONLY =
            new WorldLoopBounds(new AxisBounds.Looped(-WORLD_CHUNKS, WORLD_CHUNKS), AxisBounds.Unbounded.INSTANCE);

    private static final WorldFold PER_AXIS = WorldFolds.of(FlatShape.latticeTorus(BOUNDS, FlatShape.NO_SKEW));
    private static final WorldFold DECK_TORUS = new DeckGroupFold(FlatShape.latticeTorus(BOUNDS, FlatShape.NO_SKEW));
    private static final WorldFold SKEWED = new DeckGroupFold(FlatShape.latticeTorus(BOUNDS, SKEW_CHUNKS));
    private static final WorldFold MIRRORED =
            new DeckGroupFold(FlatShape.mirrored(BOUNDS, Direction.Axis.Z, MIRROR_LINE_CHUNK));
    private static final WorldFold CYLINDER = WorldFolds.of(FlatShape.cylinder(X_ONLY));

    private static final List<WorldFold> UNSKEWED = List.of(PER_AXIS, DECK_TORUS);
    private static final List<WorldFold> ALL = List.of(PER_AXIS, DECK_TORUS, SKEWED, MIRRORED, CYLINDER);

    private static final PanelSlot SLOT = PanelSlot.BOTTOM_RIGHT;
    private static final BlockPos CANONICAL = new BlockPos(10, 64, 3);
    private static final BlockPos ONE_LAP_AWAY = new BlockPos(10 + WORLD_BLOCKS, 64, 3);

    private static FactoryPanelPosition panel(BlockPos pos) {
        return new FactoryPanelPosition(pos, SLOT);
    }

    @Test
    void theRawPositionNamesTheCanonicalOneOneLapAway() {
        assertTrue(PER_AXIS.isOver(ONE_LAP_AWAY), ONE_LAP_AWAY + " must sit outside the bounds");
        assertEquals(CANONICAL, PER_AXIS.fold(ONE_LAP_AWAY));
    }

    @Test
    void aPanelOneLapAwayComesBackCanonicalWithItsSlot() {
        for (WorldFold fold : UNSKEWED) {
            assertEquals(panel(CANONICAL), CreateFactoryPanelFold.canonical(fold, panel(ONE_LAP_AWAY)), "in " + fold);
        }
    }

    @Test
    void thePanelLandsOnWhicheverBlockTheFoldNames() {
        for (WorldFold fold : ALL) {
            FactoryPanelPosition folded = CreateFactoryPanelFold.canonical(fold, panel(ONE_LAP_AWAY));

            assertEquals(fold.fold(ONE_LAP_AWAY), folded.pos(), "in " + fold);
            assertEquals(SLOT, folded.slot(), "in " + fold);
        }
    }

    @Test
    void anAlreadyCanonicalPanelIsGivenBackByIdentity() {
        FactoryPanelPosition canonical = panel(CANONICAL);
        for (WorldFold fold : ALL) {
            assertSame(canonical, CreateFactoryPanelFold.canonical(fold, canonical), "in " + fold);
        }
    }

    @Test
    void canonicalisingTwiceGivesTheSameInstanceBack() {
        for (WorldFold fold : ALL) {
            FactoryPanelPosition once = CreateFactoryPanelFold.canonical(fold, panel(ONE_LAP_AWAY));

            assertSame(once, CreateFactoryPanelFold.canonical(fold, once), "in " + fold);
        }
    }

    @Test
    void anUnwrappedWorldGivesThePanelBackByIdentity() {
        FactoryPanelPosition raw = panel(ONE_LAP_AWAY);

        assertSame(raw, CreateFactoryPanelFold.canonical(WorldFolds.NOOP, raw));
        assertSame(raw, CreateFactoryPanelFold.canonical((WorldFold) null, raw));
    }
}
