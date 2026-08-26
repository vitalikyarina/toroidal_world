package com.toroidalworld.player;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.accessors.NavigationShifter;
import com.toroidalworld.mixin.EntityAccessor;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public final class SeamSnap {
    public static void withPassengers(Entity entity, Vec3 shift) {
        Vec3 to = entity.position().add(shift);
        entity.setPos(to.x, to.y, to.z);
        entity.xo += shift.x;
        entity.yo += shift.y;
        entity.zo += shift.z;
        entity.xOld += shift.x;
        entity.yOld += shift.y;
        entity.zOld += shift.z;
        EntityAccessor stored = (EntityAccessor) entity;
        @Nullable Vec3 known = stored.toroidal$lastKnownPosition();
        if (known != null) {
            stored.toroidal$setLastKnownPosition(known.add(shift));
        }

        if (entity instanceof Mob mob) {
            int shiftX = (int) Math.round(shift.x);
            int shiftZ = (int) Math.round(shift.z);
            ((NavigationShifter) mob.getNavigation()).toroidal$shiftBy(shiftX, shiftZ);
            ((NavigationShifter) mob.getMoveControl()).toroidal$shiftBy(shiftX, shiftZ);
            ((NavigationShifter) mob.getLookControl()).toroidal$shiftBy(shiftX, shiftZ);
        }

        if (entity instanceof ServerPlayer rider) {
            rider.serverLevel().getChunkSource().move(rider);
        }

        for (Entity passenger : entity.getPassengers()) {
            withPassengers(passenger, shift);
        }
    }

    private SeamSnap() {
    }
}
