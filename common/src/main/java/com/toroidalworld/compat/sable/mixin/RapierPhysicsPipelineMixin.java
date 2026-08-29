package com.toroidalworld.compat.sable.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.toroidalworld.compat.sable.SableConstraintEdge;
import com.toroidalworld.compat.sable.SableConstraintEdges;
import com.toroidalworld.compat.sable.SableConstraintGraph;
import com.toroidalworld.compat.sable.SableConstraintProbe;

import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle;

@Mixin(targets = "dev.ryanhcode.sable.physics.impl.rapier.RapierPhysicsPipeline", remap = false)
public class RapierPhysicsPipelineMixin implements SableConstraintEdges {
    @Unique
    private final List<SableConstraintEdge> toroidal$constraintEdges = new ArrayList<>();

    @Override
    public List<SableConstraintEdge> toroidal$constraintEdges() {
        return this.toroidal$constraintEdges;
    }

    @Inject(method = "addConstraint", at = @At("RETURN"))
    private void toroidal$recordConstraint(PhysicsPipelineBody bodyA, PhysicsPipelineBody bodyB,
            PhysicsConstraintConfiguration<?> configuration, CallbackInfoReturnable<PhysicsConstraintHandle> cir) {
        PhysicsConstraintHandle handle = cir.getReturnValue();
        if (handle != null && bodyA != null && bodyB != null) {
            SableConstraintProbe.join(bodyA, bodyB);
            SableConstraintGraph.record((PhysicsPipeline) (Object) this, bodyA, bodyB, handle);
        }
    }
}
