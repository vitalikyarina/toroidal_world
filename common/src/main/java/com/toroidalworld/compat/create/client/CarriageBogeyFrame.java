package com.toroidalworld.compat.create.client;

import org.jspecify.annotations.Nullable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.simibubi.create.compat.trainmap.TrainMapSync.TrainMapSyncEntry;

import net.minecraft.world.phys.Vec3;

public final class CarriageBogeyFrame {
    private @Nullable TrainMapSyncEntry entry;

    private int carriageIndex;

    private @Nullable Vec3 leading;

    public static Vec3 inOneFrame(TrainMapSyncEntry entry, int carriageIndex, boolean firstBogey,
            double time, Operation<Vec3> original, LocalRef<CarriageBogeyFrame> frameRef) {
        CarriageBogeyFrame frame = frameRef.get();
        if (frame == null) {
            frame = new CarriageBogeyFrame();
            frameRef.set(frame);
        }

        Vec3 raw = original.call(entry, carriageIndex, firstBogey, time);
        return firstBogey
                ? frame.lead(entry, carriageIndex, raw)
                : frame.trail(entry, carriageIndex, time, raw, original);
    }

    private Vec3 lead(TrainMapSyncEntry leadingEntry, int leadingCarriage, Vec3 raw) {
        Vec3 canonical = TrainMapViewFold.canonical(raw);
        this.entry = leadingEntry;
        this.carriageIndex = leadingCarriage;
        this.leading = canonical;
        return canonical;
    }

    private Vec3 trail(TrainMapSyncEntry trailingEntry, int trailingCarriage, double time, Vec3 raw,
            Operation<Vec3> original) {
        Vec3 known = leading;
        if (known == null || entry != trailingEntry || carriageIndex != trailingCarriage) {
            known = TrainMapViewFold.canonical(original.call(trailingEntry, trailingCarriage, true, time));
        }

        return TrainMapViewFold.nearestTo(known, raw);
    }

    private CarriageBogeyFrame() {
    }
}
