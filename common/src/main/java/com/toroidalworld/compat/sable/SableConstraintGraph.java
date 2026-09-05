package com.toroidalworld.compat.sable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;

public final class SableConstraintGraph {
    private final List<SableConstraintEdge> edges = new ArrayList<>();

    public static List<PhysicsPipelineBody> groupOf(PhysicsPipeline pipeline, PhysicsPipelineBody start) {
        return pipeline instanceof SableConstraintGraphs holder
                ? holder.toroidal$constraintGraph().groupOf(start)
                : List.of(start);
    }

    public SableConstraintEdge record(PhysicsPipelineBody first, PhysicsPipelineBody second) {
        SableConstraintEdge edge = new SableConstraintEdge(this, first, second);
        this.edges.add(edge);
        return edge;
    }

    public void drop(SableConstraintEdge edge) {
        this.edges.removeIf(candidate -> candidate == edge);
    }

    public void dropBody(PhysicsPipelineBody body) {
        this.edges.removeIf(edge -> edge.touches(body));
    }

    public int size() {
        return this.edges.size();
    }

    public boolean isEmpty() {
        return this.edges.isEmpty();
    }

    public List<PhysicsPipelineBody> groupOf(PhysicsPipelineBody start) {
        if (this.edges.isEmpty()) {
            return List.of(start);
        }

        List<PhysicsPipelineBody> group = new ArrayList<>();
        Set<PhysicsPipelineBody> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        group.add(start);
        seen.add(start);
        for (int index = 0; index < group.size(); index++) {
            PhysicsPipelineBody body = group.get(index);
            for (SableConstraintEdge edge : this.edges) {
                PhysicsPipelineBody other = edge.otherEnd(body);
                if (other != null && seen.add(other)) {
                    group.add(other);
                }
            }
        }

        return group;
    }
}
