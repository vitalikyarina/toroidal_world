package com.toroidalworld.compat.sable;

import org.jspecify.annotations.Nullable;

import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;

public record SableConstraintEdge(SableConstraintGraph graph, PhysicsPipelineBody first, PhysicsPipelineBody second) {
    public void drop() {
        this.graph.drop(this);
    }

    boolean touches(PhysicsPipelineBody body) {
        return body == this.first || body == this.second;
    }

    @Nullable PhysicsPipelineBody otherEnd(PhysicsPipelineBody body) {
        if (body == this.first) {
            return this.second;
        }

        return body == this.second ? this.first : null;
    }
}
