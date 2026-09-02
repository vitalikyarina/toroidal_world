package com.toroidalworld.compat.create;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.core.BlockPos;

class CanonicalPositionKeysTest {
    private static final int WORLD_CHUNKS = 16;
    private static final int WORLD_BLOCKS = WORLD_CHUNKS * 2 * 16;

    private static final WorldLoopBounds BOUNDS =
            new WorldLoopBounds(-WORLD_CHUNKS, WORLD_CHUNKS, -WORLD_CHUNKS, WORLD_CHUNKS);
    private static final WorldFold FOLD = WorldFolds.of(FlatShape.latticeTorus(BOUNDS, FlatShape.NO_SKEW));

    private static final BlockPos CANONICAL = new BlockPos(10, 64, 3);
    private static final BlockPos RAW = new BlockPos(10 + WORLD_BLOCKS, 64, 3);
    private static final BlockPos NEIGHBOUR = new BlockPos(-20, 64, 7);

    private static final String VALUE = "behaviour";
    private static final String ABSENT = "absent";

    private static Set<BlockPos> canonicalKeys(BlockPos... positions) {
        Set<BlockPos> keys = new CanonicalPositionKeys.CanonicalSet(FOLD);
        for (BlockPos position : positions) {
            keys.add(position);
        }

        return keys;
    }

    private static Map<BlockPos, String> canonicalEntries(BlockPos key) {
        Map<BlockPos, String> entries = new CanonicalPositionKeys.CanonicalMap<>(FOLD);
        entries.put(key, VALUE);

        return entries;
    }

    @Test
    void theRawPositionNamesTheCanonicalOneOneLapAway() {
        assertTrue(FOLD.isOver(RAW), RAW + " must sit outside the bounds");
        assertEquals(CANONICAL, FOLD.fold(RAW));
        assertFalse(FOLD.isOver(NEIGHBOUR), NEIGHBOUR + " must sit inside the bounds");
    }

    @Test
    void removeAllAnswersTheSameOnBothSizeBranches() {
        Set<BlockPos> largerThanItsArgument = canonicalKeys(CANONICAL, NEIGHBOUR);
        Set<BlockPos> noLargerThanItsArgument = canonicalKeys(CANONICAL);

        assertTrue(largerThanItsArgument.removeAll(Set.of(RAW)));
        assertTrue(noLargerThanItsArgument.removeAll(Set.of(RAW)));

        assertEquals(Set.of(NEIGHBOUR), largerThanItsArgument);
        assertTrue(noLargerThanItsArgument.isEmpty());
    }

    @Test
    void retainAllKeepsTheKeyItsArgumentNamesRaw() {
        Set<BlockPos> keys = canonicalKeys(CANONICAL, NEIGHBOUR);

        assertTrue(keys.retainAll(Set.of(RAW)));

        assertEquals(Set.of(CANONICAL), keys);
    }

    @Test
    void addAllAndContainsAllFoldEveryElement() {
        Set<BlockPos> keys = canonicalKeys();

        assertTrue(keys.addAll(List.of(RAW, CANONICAL)));

        assertEquals(Set.of(CANONICAL), keys);
        assertTrue(keys.containsAll(Set.of(RAW, CANONICAL)));
    }

    @Test
    void theIteratorYieldsCanonicalKeysAndRemovesTheYieldedOne() {
        Set<BlockPos> keys = canonicalKeys(RAW);
        Iterator<BlockPos> yielded = keys.iterator();

        assertEquals(CANONICAL, yielded.next());
        yielded.remove();

        assertTrue(keys.isEmpty());
    }

    @Test
    void keySetRemoveDeletesTheMappingNamedRaw() {
        Map<BlockPos, String> entries = canonicalEntries(CANONICAL);

        assertTrue(entries.keySet().remove(RAW));

        assertTrue(entries.isEmpty());
    }

    @Test
    void keySetRemoveAllAndRetainAllFoldTheirArgument() {
        Map<BlockPos, String> emptied = canonicalEntries(CANONICAL);
        Map<BlockPos, String> retained = canonicalEntries(CANONICAL);

        assertTrue(emptied.keySet().removeAll(Set.of(RAW)));
        assertFalse(retained.keySet().retainAll(Set.of(RAW)));

        assertTrue(emptied.isEmpty());
        assertEquals(Set.of(CANONICAL), retained.keySet());
    }

    @Test
    void entrySetContainsAndRemoveFoldTheEntryKey() {
        Map<BlockPos, String> entries = canonicalEntries(CANONICAL);
        Map.Entry<BlockPos, String> namedRaw = new AbstractMap.SimpleEntry<>(RAW, VALUE);

        assertTrue(entries.entrySet().contains(namedRaw));
        assertTrue(entries.entrySet().remove(namedRaw));

        assertTrue(entries.isEmpty());
    }

    @Test
    void valuesStaysALiveViewOfTheBackingMap() {
        Map<BlockPos, String> entries = canonicalEntries(RAW);

        assertEquals(Set.of(CANONICAL), entries.keySet());
        assertTrue(entries.values().remove(VALUE));

        assertTrue(entries.isEmpty());
    }

    @Test
    void theMapDefaultsFoldTheKey() {
        Map<BlockPos, String> entries = canonicalEntries(CANONICAL);

        assertEquals(VALUE, entries.getOrDefault(RAW, ABSENT));
        assertEquals(VALUE, entries.putIfAbsent(RAW, ABSENT));
        assertEquals(VALUE, entries.computeIfAbsent(RAW, position -> ABSENT));
        assertEquals(1, entries.size());
        assertTrue(entries.remove(RAW, VALUE));
    }
}
