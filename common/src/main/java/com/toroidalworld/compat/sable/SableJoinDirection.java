package com.toroidalworld.compat.sable;

import java.util.List;

import com.toroidalworld.core.DeckTransformation;
import com.toroidalworld.core.WorldFold;

import net.minecraft.world.phys.Vec3;

final class SableJoinDirection {
    record Choice(boolean movingIsB, DeckTransformation lap) {
    }

    static DeckTransformation lapOnto(WorldFold fold, Vec3 anchor, Vec3 moving) {
        return fold.nearestCopyTransformation(anchor, moving);
    }

    static Choice choose(WorldFold fold, DeckTransformation lapForB, List<Vec3> groupA, List<Vec3> groupB) {
        if (seatsInside(fold, lapForB, groupA, groupB)) {
            return new Choice(true, lapForB);
        }

        DeckTransformation lapForA = lapForB.inverse();
        if (seatsInside(fold, lapForA, groupB, groupA)) {
            return new Choice(false, lapForA);
        }

        boolean movingIsB = groupB.size() <= groupA.size();
        return new Choice(movingIsB, movingIsB ? lapForB : lapForA);
    }

    private static boolean seatsInside(WorldFold fold, DeckTransformation lap, List<Vec3> stationary,
            List<Vec3> moving) {
        int counted = stationary.size() + moving.size();
        if (counted == 0) {
            return false;
        }

        double x = 0.0;
        double y = 0.0;
        double z = 0.0;
        for (Vec3 position : stationary) {
            x += position.x;
            y += position.y;
            z += position.z;
        }

        for (Vec3 position : moving) {
            Vec3 seated = lap.apply(position);
            x += seated.x;
            y += seated.y;
            z += seated.z;
        }

        return !fold.isOver(new Vec3(x / counted, y / counted, z / counted));
    }

    private SableJoinDirection() {
    }
}
