package com.toroidalworld.player;

import com.toroidalworld.accessors.NavigationShifter;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

// The one way to move an entity across the seam: the whole passenger stack shifts by the same vector in the same
// moment, or vehicle and rider spend time a world apart and every distance check between them lies. absSnapTo also
// resets the old position, so nothing interpolates across the whole world; a player's chunk source follows, because
// the shift is a whole-world jump the tracker must see at once. A mob's navigation shifts too — its path was laid out
// in the coordinate space the mob just left (see NavigationShifter).
public final class SeamSnap {
    public static void withPassengers(Entity entity, Vec3 shift) {
        Vec3 to = entity.position().add(shift);
        entity.absSnapTo(to.x, to.y, to.z);
        if (entity instanceof Mob mob) {
            int shiftX = (int) Math.round(shift.x);
            int shiftZ = (int) Math.round(shift.z);
            ((NavigationShifter) mob.getNavigation()).toroidal$shiftBy(shiftX, shiftZ);
            ((NavigationShifter) mob.getMoveControl()).toroidal$shiftBy(shiftX, shiftZ);
            ((NavigationShifter) mob.getLookControl()).toroidal$shiftBy(shiftX, shiftZ);
        }

        if (entity instanceof ServerPlayer rider) {
            rider.level().getChunkSource().move(rider);
        }

        for (Entity passenger : entity.getPassengers()) {
            withPassengers(passenger, shift);
        }
    }

    private SeamSnap() {
    }
}
