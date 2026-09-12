package com.toroidalworld.compat.sable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.toroidalworld.core.DeckTransformation;

import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;

import net.minecraft.server.level.ServerLevel;

public final class SableBodyShift {
    public interface Listener {
        void onGroupShifted(ServerLevel level, List<PhysicsPipelineBody> group, DeckTransformation seat);
    }

    private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();

    public static void register(Listener listener) {
        LISTENERS.add(listener);
    }

    static void fire(ServerLevel level, List<PhysicsPipelineBody> group, DeckTransformation seat) {
        for (Listener listener : LISTENERS) {
            listener.onGroupShifted(level, group, seat);
        }
    }

    private SableBodyShift() {
    }
}
