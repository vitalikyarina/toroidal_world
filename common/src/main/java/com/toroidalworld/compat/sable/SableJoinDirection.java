package com.toroidalworld.compat.sable;

import java.util.List;

import com.toroidalworld.core.WorldFold;

import net.minecraft.world.phys.Vec3;

final class SableJoinDirection {
    record Choice(boolean movingIsB, Vec3 lap) {
    }

    static Vec3 lapOnto(WorldFold fold, Vec3 anchor, Vec3 moving) {
        Vec3 nearest = fold.nearestCopy(anchor, moving);
        return new Vec3(nearest.x - moving.x, 0.0, nearest.z - moving.z);
    }

    private static Vec3 reversed(Vec3 lap) {
        return new Vec3(negated(lap.x), 0.0, negated(lap.z));
    }

    private static double negated(double lap) {
        return lap == 0.0 ? 0.0 : -lap;
    }

    static Choice choose(WorldFold fold, Vec3 lapForB, List<Vec3> groupA, List<Vec3> groupB) {
        if (seatsInside(fold, lapForB, groupA, groupB)) {
            return new Choice(true, lapForB);
        }

        Vec3 lapForA = reversed(lapForB);
        if (seatsInside(fold, lapForA, groupB, groupA)) {
            return new Choice(false, lapForA);
        }

        boolean movingIsB = groupB.size() <= groupA.size();
        return new Choice(movingIsB, movingIsB ? lapForB : lapForA);
    }

    private static boolean seatsInside(WorldFold fold, Vec3 lap, List<Vec3> stationary, List<Vec3> moving) {
        int counted = stationary.size() + moving.size();
        if (counted == 0) {
            return false;
        }

        double x = lap.x * moving.size();
        double y = 0.0;
        double z = lap.z * moving.size();
        for (Vec3 position : stationary) {
            x += position.x;
            y += position.y;
            z += position.z;
        }

        for (Vec3 position : moving) {
            x += position.x;
            y += position.y;
            z += position.z;
        }

        return !fold.isOver(new Vec3(x / counted, y / counted, z / counted));
    }

    private SableJoinDirection() {
    }
}
