package com.toroidalworld.compat.sable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle;

public final class SableConstraintGraph {
    public static void record(PhysicsPipeline pipeline, PhysicsPipelineBody first, PhysicsPipelineBody second,
            PhysicsConstraintHandle handle) {
        if (!(pipeline instanceof SableConstraintEdges holder)) {
            return;
        }

        holder.toroidal$constraintEdges().add(new SableConstraintEdge(first, second, handle));
    }

    public static List<PhysicsPipelineBody> groupOf(PhysicsPipeline pipeline, PhysicsPipelineBody start) {
        if (!(pipeline instanceof SableConstraintEdges holder)) {
            return List.of(start);
        }

        List<SableConstraintEdge> edges = holder.toroidal$constraintEdges();
        if (edges.isEmpty()) {
            return List.of(start);
        }

        edges.removeIf(edge -> !edge.live());
        List<PhysicsPipelineBody> group = new ArrayList<>();
        Set<PhysicsPipelineBody> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        group.add(start);
        seen.add(start);
        for (int index = 0; index < group.size(); index++) {
            PhysicsPipelineBody body = group.get(index);
            for (SableConstraintEdge edge : edges) {
                PhysicsPipelineBody other = edge.otherEnd(body);
                if (other != null && seen.add(other)) {
                    group.add(other);
                }
            }
        }

        return group;
    }

    private SableConstraintGraph() {
    }
}
