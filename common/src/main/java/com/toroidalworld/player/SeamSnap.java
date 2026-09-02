package com.toroidalworld.player;

import com.toroidalworld.accessors.NavigationShifter;
import com.toroidalworld.core.DeckTransformation;
import com.toroidalworld.core.SeamTransform;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public final class SeamSnap {
    public static void withPassengers(Entity entity, DeckTransformation lap) {
        SeamTransform applied = lap.blocks();
        Vec3 from = entity.position();
        Vec3 to = lap.apply(from);
        entity.setPos(to.x, to.y, to.z);
        entity.xo = applied.applyX(entity.xo);
        entity.zo = applied.applyZ(entity.zo);
        entity.xOld = applied.applyX(entity.xOld);
        entity.zOld = applied.applyZ(entity.zOld);

        if (entity instanceof Mob mob) {
            int shiftX = (int) Math.round(to.x - from.x);
            int shiftZ = (int) Math.round(to.z - from.z);
            ((NavigationShifter) mob.getNavigation()).toroidal$shiftBy(shiftX, shiftZ);
            ((NavigationShifter) mob.getMoveControl()).toroidal$shiftBy(shiftX, shiftZ);
            ((NavigationShifter) mob.getLookControl()).toroidal$shiftBy(shiftX, shiftZ);
        }

        if (entity instanceof ServerPlayer rider) {
            rider.serverLevel().getChunkSource().move(rider);
        }

        for (Entity passenger : entity.getPassengers()) {
            withPassengers(passenger, lap);
        }
    }

    private SeamSnap() {
    }
}
