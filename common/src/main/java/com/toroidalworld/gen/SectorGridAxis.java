package com.toroidalworld.gen;

import com.toroidalworld.core.WrapDomain;

// One horizontal axis of a random-spread placement's sector grid, folded into the world. Vanilla's grid is anchored at
// absolute zero — floorDiv of a chunk coordinate names its cell — and a search ring names cells by their offset from
// the origin's cell. On a closed axis an offset is a residue: the cell it names is the one its folded image lands in,
// so the ring at radius r holds the cells r sectors away through the seam as well as the flat way, and a treasure
// three cells past the seam is met at ring three rather than at a ring the width of the world.
//
// The offsets [-cap, cap] with cap = count / 2 name every cell of the axis at its folded distance and nothing twice,
// except the exact antipode of an even count, which two offsets share — the second probe costs one already-cached
// presence check and changes no answer, which is cheaper than deduplicating every ring.
//
// The world's edge cells may be partial — the width need not divide by the spacing — so the chunk a cell answers a
// probe at is clamped to ground the world holds. The cell's potential chunk can still fall past the bounds (the spread
// reaches anywhere in a full-sized cell); that candidate names no possible structure and is the caller's to turn away,
// by the same test as any other out-of-bounds chunk.
//
// An axis that does not close keeps vanilla's arithmetic exactly: offsets step from the origin chunk itself, and the
// cap is a count no search radius reaches.
public final class SectorGridAxis {
    private final WrapDomain domain;
    private final int spacing;
    private final int originChunk;
    private final boolean closes;
    private final int gridMin;
    private final int gridCount;
    private final int originGrid;
    private final int offsetCap;

    private SectorGridAxis(WrapDomain domain, int spacing, int originChunk) {
        this.domain = domain;
        this.spacing = spacing;
        this.originChunk = originChunk;
        this.closes = domain.stepsToCoverTheWorld(spacing) != Integer.MAX_VALUE;
        if (this.closes) {
            int lowestCell = Math.floorDiv(domain.lowerBound, spacing);
            int highestCell = Math.floorDiv(domain.upperBound - 1, spacing);
            this.gridMin = lowestCell;
            this.gridCount = highestCell - lowestCell + 1;
            this.originGrid = Math.floorDiv(originChunk, spacing);
            this.offsetCap = this.gridCount / 2;
        } else {
            this.gridMin = 0;
            this.gridCount = 0;
            this.originGrid = 0;
            this.offsetCap = Integer.MAX_VALUE;
        }
    }

    public static SectorGridAxis of(WrapDomain chunkDomain, int spacing, int originChunk) {
        return new SectorGridAxis(chunkDomain, spacing, originChunk);
    }

    // The farthest folded offset this axis has: a cell past it is a nearer copy of a cell already named.
    public int offsetCap() {
        return offsetCap;
    }

    // The chunk coordinate at which the folded offset's cell answers a probe — any chunk inside the cell serves, the
    // probe reads the cell off it by floorDiv. Clamped up to the bounds where the low edge cell is partial; the high
    // edge cell's own start is inside the bounds by construction.
    public int probeChunk(int offset) {
        if (!closes) {
            return originChunk + spacing * offset;
        }

        int cell = gridMin + Math.floorMod(originGrid + offset - gridMin, gridCount);
        return Math.max(cell * spacing, domain.lowerBound);
    }
}
