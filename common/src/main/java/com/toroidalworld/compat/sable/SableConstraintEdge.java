package com.toroidalworld.compat.sable;

import org.jspecify.annotations.Nullable;

import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle;

public record SableConstraintEdge(PhysicsPipelineBody first, PhysicsPipelineBody second, PhysicsConstraintHandle handle) {
    boolean live() {
        return !this.first.isRemoved() && !this.second.isRemoved() && this.handle.isValid();
    }

    @Nullable PhysicsPipelineBody otherEnd(PhysicsPipelineBody body) {
        if (body == this.first) {
            return this.second;
        }

        return body == this.second ? this.first : null;
    }
}
