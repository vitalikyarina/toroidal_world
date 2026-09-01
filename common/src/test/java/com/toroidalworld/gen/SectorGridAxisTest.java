package com.toroidalworld.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WrapDomain;

class SectorGridAxisTest {
    private static final long SEED = 0x5EEDL;

    private static final List<WrapDomain> DOMAINS = List.of(
            new WrapDomain(-32, 32),
            new WrapDomain(-2, 3),
            new WrapDomain(0, 1),
            new WrapDomain(-48, 16),
            new WrapDomain(0, 16));

    private static final int[] SPACINGS = {1, 2, 3, 4, 5, 7, 16, 64, 70};

    private static List<Integer> origins(WrapDomain domain) {
        Random random = new Random(SEED);
        List<Integer> origins = new ArrayList<>(List.of(domain.lowerBound, domain.upperBound - 1));
        for (int i = 0; i < 3; i++) {
            origins.add(domain.lowerBound + random.nextInt(domain.domainLength));
        }

        return origins;
    }

    private static TreeSet<Integer> walkCells(WrapDomain domain, int spacing) {
        TreeSet<Integer> cells = new TreeSet<>();
        for (int chunk = domain.lowerBound; chunk < domain.upperBound; chunk++) {
            cells.add(Math.floorDiv(chunk, spacing));
        }

        return cells;
    }

    private static int foldedCellDistance(int originCell, int cell, int cellCount) {
        int distance = Integer.MAX_VALUE;
        for (int lap = -2; lap <= 2; lap++) {
            distance = Math.min(distance, Math.abs(cell - originCell + lap * cellCount));
        }

        return distance;
    }

    private static String in(WrapDomain domain, int spacing, int origin) {
        return "in [" + domain.lowerBound + ", " + domain.upperBound + ") spacing " + spacing + " origin " + origin;
    }

    private interface ClosedAxisCheck {
        void run(WrapDomain domain, int spacing, int origin, SectorGridAxis axis, TreeSet<Integer> cells);
    }

    private static void forEachClosedAxis(ClosedAxisCheck check) {
        for (WrapDomain domain : DOMAINS) {
            for (int spacing : SPACINGS) {
                TreeSet<Integer> cells = walkCells(domain, spacing);
                for (int origin : origins(domain)) {
                    check.run(domain, spacing, origin, SectorGridAxis.of(domain, spacing, origin), cells);
                }
            }
        }
    }

    @Test
    void offsetCapIsHalfTheCellCount() {
        forEachClosedAxis((domain, spacing, origin, axis, cells) ->
                assertEquals(cells.size() / 2, axis.offsetCap(), in(domain, spacing, origin)));
    }

    @Test
    void everyOffsetNamesItsCellAtTheFoldedDistance() {
        forEachClosedAxis((domain, spacing, origin, axis, cells) -> {
            int originCell = Math.floorDiv(origin, spacing);
            for (int offset = -axis.offsetCap(); offset <= axis.offsetCap(); offset++) {
                int cell = Math.floorDiv(axis.probeChunk(offset), spacing);
                assertEquals(Math.abs(offset), foldedCellDistance(originCell, cell, cells.size()),
                        in(domain, spacing, origin) + " offset " + offset);
            }
        });
    }

    @Test
    void offsetsNameEveryCellOnceExceptTheEvenAntipode() {
        forEachClosedAxis((domain, spacing, origin, axis, cells) -> {
            Map<Integer, Integer> named = new HashMap<>();
            for (int offset = -axis.offsetCap(); offset <= axis.offsetCap(); offset++) {
                named.merge(Math.floorDiv(axis.probeChunk(offset), spacing), 1, Integer::sum);
            }

            String context = in(domain, spacing, origin);
            assertEquals(cells, new TreeSet<>(named.keySet()), context);
            if (cells.size() % 2 != 0) {
                named.forEach((cell, count) -> assertEquals(1, count, context + " cell " + cell));
                return;
            }

            int cap = axis.offsetCap();
            int antipode = Math.floorDiv(axis.probeChunk(cap), spacing);
            assertEquals(antipode, Math.floorDiv(axis.probeChunk(-cap), spacing), context);
            named.forEach((cell, count) ->
                    assertEquals(cell == antipode ? 2 : 1, count, context + " cell " + cell));
        });
    }

    @Test
    void probeChunkAnswersInsideTheWorld() {
        forEachClosedAxis((domain, spacing, origin, axis, cells) -> {
            for (int offset = -axis.offsetCap(); offset <= axis.offsetCap(); offset++) {
                int probe = axis.probeChunk(offset);
                String context = in(domain, spacing, origin) + " offset " + offset + " probe " + probe;
                assertTrue(probe >= domain.lowerBound && probe < domain.upperBound, context);
                assertTrue(cells.contains(Math.floorDiv(probe, spacing)), context);
            }
        });
    }

    @Test
    void anOpenAxisKeepsVanillaArithmetic() {
        WrapDomain open = new WrapDomain.Noop();
        for (int spacing : SPACINGS) {
            for (int origin : List.of(-1000, 0, 37)) {
                SectorGridAxis axis = SectorGridAxis.of(open, spacing, origin);
                assertEquals(Integer.MAX_VALUE, axis.offsetCap());
                for (int offset = -50; offset <= 50; offset++) {
                    assertEquals(origin + spacing * offset, axis.probeChunk(offset),
                            "spacing " + spacing + " origin " + origin + " offset " + offset);
                }
            }
        }
    }
}
