package com.toroidalworld.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.toroidalworld.accessors.TransformerHolder;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.world.level.ChunkPos;

@Timeout(60)
class ChunkTrackingDifferenceTest {
    private static final int MIN_CHUNK = -16;
    private static final int MAX_CHUNK = 15;
    private static final int VIEW_DISTANCE = 8;
    private static final int NEIGHBOUR_BUFFER = 1;

    private static final WorldFold CYLINDER = WorldFolds.of(FlatShape.cylinder(
            new WorldLoopBounds(new AxisBounds.Looped(MIN_CHUNK, MAX_CHUNK + 1), AxisBounds.Unbounded.INSTANCE)));
    private static final WorldFold TORUS =
            WorldFolds.of(FlatShape.latticeTorus(WorldLoopBounds.ofWidth(MAX_CHUNK - MIN_CHUNK + 1), 0));

    @Test
    void aJumpAlongTheUnboundedAxisEntersTheDestinationAndLeavesTheOrigin() {
        ChunkTrackingView.Positioned previous = view(CYLINDER, 0, 0);
        ChunkTrackingView.Positioned next = view(CYLINDER, 0, 1_250_000);

        Set<ChunkPos> entered = new HashSet<>();
        Set<ChunkPos> left = new HashSet<>();
        ChunkTrackingView.difference(previous, next, entered::add, left::add);

        assertEquals(viewChunks(CYLINDER, 0, 1_250_000), entered);
        assertEquals(viewChunks(CYLINDER, 0, 0), left);
    }

    @Test
    void aStepAcrossTheSeamEntersAndLeavesOnlyWhatTheTwoViewsDoNotShare() {
        ChunkTrackingView.Positioned previous = view(TORUS, 15, 0);
        ChunkTrackingView.Positioned next = view(TORUS, -15, 0);

        Set<ChunkPos> entered = new HashSet<>();
        Set<ChunkPos> left = new HashSet<>();
        ChunkTrackingView.difference(previous, next, entered::add, left::add);

        Set<ChunkPos> previousChunks = viewChunks(TORUS, 15, 0);
        Set<ChunkPos> nextChunks = viewChunks(TORUS, -15, 0);

        assertEquals(without(nextChunks, previousChunks), entered);
        assertEquals(without(previousChunks, nextChunks), left);
    }

    private static ChunkTrackingView.Positioned view(WorldFold fold, int centerX, int centerZ) {
        ChunkTrackingView.Positioned view =
                (ChunkTrackingView.Positioned) ChunkTrackingView.of(new ChunkPos(centerX, centerZ), VIEW_DISTANCE);
        ((TransformerHolder) (Object) view).toroidal$setTransformer(fold);

        assertSame(fold, ((TransformerHolder) (Object) view).toroidal$transformer(), "the view kept no transformer");
        return view;
    }

    private static Set<ChunkPos> viewChunks(WorldFold fold, int centerX, int centerZ) {
        Set<ChunkPos> chunks = new HashSet<>();
        int radius = VIEW_DISTANCE + 1;

        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                long deltaX = Math.max(0, Math.abs(x - centerX) - NEIGHBOUR_BUFFER);
                long deltaZ = Math.max(0, Math.abs(z - centerZ) - NEIGHBOUR_BUFFER);
                long far = Math.max(0, Math.max(deltaX, deltaZ) - NEIGHBOUR_BUFFER);
                long near = Math.min(deltaX, deltaZ);
                if (near * near + far * far < (long) VIEW_DISTANCE * VIEW_DISTANCE) {
                    chunks.add(fold.fold(new ChunkPos(x, z)));
                }
            }
        }

        return chunks;
    }

    private static Set<ChunkPos> without(Set<ChunkPos> chunks, Set<ChunkPos> removed) {
        Set<ChunkPos> rest = new HashSet<>(chunks);
        rest.removeAll(removed);
        return rest;
    }
}
