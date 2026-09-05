package com.toroidalworld.compat.sable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;

public final class SableConstraintGraph {
    private final List<SableConstraintEdge> edges = new ArrayList<>();
    private @Nullable Map<PhysicsPipelineBody, List<PhysicsPipelineBody>> groups;

    public static List<PhysicsPipelineBody> groupOf(PhysicsPipeline pipeline, PhysicsPipelineBody start) {
        return pipeline instanceof SableConstraintGraphHolder holder
                ? holder.toroidal$constraintGraph().groupOf(start)
                : List.of(start);
    }

    public SableConstraintEdge record(PhysicsPipelineBody first, PhysicsPipelineBody second) {
        SableConstraintEdge edge = new SableConstraintEdge(this, first, second);
        this.edges.add(edge);
        this.groups = null;
        return edge;
    }

    public void drop(SableConstraintEdge edge) {
        if (this.edges.removeIf(candidate -> candidate == edge)) {
            this.groups = null;
        }
    }

    public void dropBody(PhysicsPipelineBody body) {
        if (this.edges.removeIf(edge -> edge.touches(body))) {
            this.groups = null;
        }
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

        Map<PhysicsPipelineBody, List<PhysicsPipelineBody>> groups = this.groups;
        if (groups == null) {
            groups = this.walkGroups();
            this.groups = groups;
        }

        List<PhysicsPipelineBody> group = groups.get(start);
        return group == null ? List.of(start) : group;
    }

    private Map<PhysicsPipelineBody, List<PhysicsPipelineBody>> walkGroups() {
        Map<PhysicsPipelineBody, List<PhysicsPipelineBody>> neighbours = new IdentityHashMap<>();
        for (SableConstraintEdge edge : this.edges) {
            neighbours.computeIfAbsent(edge.first(), body -> new ArrayList<>()).add(edge.second());
            neighbours.computeIfAbsent(edge.second(), body -> new ArrayList<>()).add(edge.first());
        }

        Map<PhysicsPipelineBody, List<PhysicsPipelineBody>> groups = new IdentityHashMap<>();
        Set<PhysicsPipelineBody> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (PhysicsPipelineBody start : neighbours.keySet()) {
            if (!seen.add(start)) {
                continue;
            }

            List<PhysicsPipelineBody> group = new ArrayList<>();
            group.add(start);
            for (int index = 0; index < group.size(); index++) {
                for (PhysicsPipelineBody other : neighbours.get(group.get(index))) {
                    if (seen.add(other)) {
                        group.add(other);
                    }
                }
            }

            List<PhysicsPipelineBody> members = List.copyOf(group);
            for (PhysicsPipelineBody member : members) {
                groups.put(member, members);
            }
        }

        return groups;
    }
}
