package com.toroidalworld.compat.create;

import com.toroidalworld.core.WrapDomain;

public final class TrainMapLaps {
    public record Range(int lowest, int highest, int needed) {
        public int kept() {
            return Math.max(0, highest - lowest + 1);
        }

        public boolean capped() {
            return needed > kept();
        }
    }

    public static Range range(WrapDomain domain, int start, int span, int copiesEachSide) {
        if (domain.domainLength <= 0) {
            return new Range(0, 0, 1);
        }

        int touchedLowest = Math.floorDiv(start - domain.upperBound, domain.domainLength) + 1;
        int touchedHighest = Math.floorDiv(start + span - 1 - domain.lowerBound, domain.domainLength);
        int needed = touchedHighest - touchedLowest + 1;
        int spread = Math.max(0, copiesEachSide);
        return new Range(Math.max(touchedLowest, -spread), Math.min(touchedHighest, spread), needed);
    }

    private TrainMapLaps() {
    }
}
