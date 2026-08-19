package com.toroidalworld.gen;

import com.toroidalworld.core.WrapDomain;

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

    public int offsetCap() {
        return offsetCap;
    }

    public int probeChunk(int offset) {
        if (!closes) {
            return originChunk + spacing * offset;
        }

        int cell = gridMin + Math.floorMod(originGrid + offset - gridMin, gridCount);
        return Math.max(cell * spacing, domain.lowerBound);
    }
}
