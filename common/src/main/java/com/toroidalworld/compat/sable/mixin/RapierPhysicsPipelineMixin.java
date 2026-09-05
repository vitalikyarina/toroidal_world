package com.toroidalworld.compat.sable.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.sugar.Local;
import com.toroidalworld.compat.sable.SableConstraintEdgeHolder;
import com.toroidalworld.compat.sable.SableConstraintGraph;
import com.toroidalworld.compat.sable.SableConstraintGraphHolder;
import com.toroidalworld.compat.sable.SableConstraintJoin;
import com.toroidalworld.compat.sable.SableMotorGoal;
import com.toroidalworld.compat.sable.SableMotorGoalHolder;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle;

import net.minecraft.server.level.ServerLevel;

@Mixin(targets = "dev.ryanhcode.sable.physics.impl.rapier.RapierPhysicsPipeline", remap = false)
public class RapierPhysicsPipelineMixin implements SableConstraintGraphHolder {
    @Shadow
    @Final
    private ServerLevel level;

    @Unique
    private final SableConstraintGraph toroidal$constraintGraph = new SableConstraintGraph();

    @Override
    public SableConstraintGraph toroidal$constraintGraph() {
        return this.toroidal$constraintGraph;
    }

    @ModifyVariable(method = "addConstraint", at = @At("HEAD"), argsOnly = true)
    private PhysicsConstraintConfiguration<?> toroidal$seatBeforeConstraint(
            PhysicsConstraintConfiguration<?> configuration,
            @Local(argsOnly = true, ordinal = 0) PhysicsPipelineBody bodyA,
            @Local(argsOnly = true, ordinal = 1) PhysicsPipelineBody bodyB) {
        return SableConstraintJoin.seat(this.level, (PhysicsPipeline) (Object) this, bodyA, bodyB, configuration);
    }

    @Inject(method = "addConstraint", at = @At("RETURN"))
    private void toroidal$recordConstraint(PhysicsPipelineBody bodyA, PhysicsPipelineBody bodyB,
            PhysicsConstraintConfiguration<?> configuration, CallbackInfoReturnable<PhysicsConstraintHandle> cir) {
        PhysicsConstraintHandle handle = cir.getReturnValue();
        WorldFold fold = WorldLoopAttachments.wrappedTransformerOf(this.level);
        if (handle == null || fold == null) {
            return;
        }

        if (bodyA != null && bodyB != null) {
            if (handle instanceof SableConstraintEdgeHolder holder) {
                holder.toroidal$constraintEdge(this.toroidal$constraintGraph.record(bodyA, bodyB));
            }

            return;
        }

        if (bodyA == null && bodyB != null && handle instanceof SableMotorGoalHolder holder) {
            holder.toroidal$motorGoal(SableMotorGoal.of(fold, bodyB, configuration));
        }
    }
}
