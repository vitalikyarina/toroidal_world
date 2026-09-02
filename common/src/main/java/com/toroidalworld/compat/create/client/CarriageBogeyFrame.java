package com.toroidalworld.compat.create.client;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.simibubi.create.compat.trainmap.TrainMapSync.TrainMapSyncEntry;
import com.toroidalworld.core.WorldFold;

import net.minecraft.world.phys.Vec3;

public final class CarriageBogeyFrame {
    private @Nullable Object train;

    private int carriageIndex;

    private @Nullable Vec3 leading;

    public static Vec3 inOneFrame(TrainMapSyncEntry entry, int carriageIndex, boolean firstBogey,
            double time, Operation<Vec3> original, LocalRef<CarriageBogeyFrame> frameRef) {
        CarriageBogeyFrame frame = frameRef.get();
        if (frame == null) {
            frame = new CarriageBogeyFrame();
            frameRef.set(frame);
        }

        WorldFold transformer = TrainMapViewFold.transformer();
        Vec3 raw = original.call(entry, carriageIndex, firstBogey, time);
        return firstBogey
                ? frame.lead(transformer, entry, carriageIndex, raw)
                : frame.trail(transformer, entry, carriageIndex, raw,
                        () -> original.call(entry, carriageIndex, true, time));
    }

    public Vec3 lead(@Nullable WorldFold transformer, Object leadingTrain, int leadingCarriage, Vec3 raw) {
        Vec3 canonical = transformer == null ? raw : transformer.fold(raw);
        this.train = leadingTrain;
        this.carriageIndex = leadingCarriage;
        this.leading = canonical;
        return canonical;
    }

    public Vec3 trail(@Nullable WorldFold transformer, Object trailingTrain, int trailingCarriage, Vec3 raw,
            Supplier<Vec3> leadingBogey) {
        Vec3 known = this.leading;
        if (known == null || this.train != trailingTrain || this.carriageIndex != trailingCarriage) {
            Vec3 rawLeading = leadingBogey.get();
            known = transformer == null ? rawLeading : transformer.fold(rawLeading);
        }

        return transformer == null ? raw : transformer.nearestCopy(known, raw);
    }
}
