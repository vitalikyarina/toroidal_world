package com.toroidalworld.compat.sable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.joml.Vector3dc;

import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;

import net.minecraft.server.level.ServerLevel;

public final class SableBodyShift {
    public interface Listener {
        void onGroupShifted(ServerLevel level, List<PhysicsPipelineBody> group, Vector3dc lap);
    }

    private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();

    public static void register(Listener listener) {
        LISTENERS.add(listener);
    }

    static void fire(ServerLevel level, List<PhysicsPipelineBody> group, Vector3dc lap) {
        for (Listener listener : LISTENERS) {
            listener.onGroupShifted(level, group, lap);
        }
    }

    private SableBodyShift() {
    }
}
