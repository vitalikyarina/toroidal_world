package com.toroidalworld.compat.sable.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.toroidalworld.compat.sable.SableConstraintGraphs;
import com.toroidalworld.compat.sable.SablePoseFold;

import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;

@Mixin(value = SubLevelPhysicsSystem.class, remap = false)
public class SubLevelPhysicsSystemMixin {
    @Shadow
    @Final
    private PhysicsPipeline pipeline;

    @WrapOperation(
            method = "updatePose",
            at = @At(value = "INVOKE",
                    target = "Ldev/ryanhcode/sable/api/physics/PhysicsPipeline;readPose(Ldev/ryanhcode/sable/sublevel/ServerSubLevel;Ldev/ryanhcode/sable/companion/math/Pose3d;)Ldev/ryanhcode/sable/companion/math/Pose3d;"))
    private Pose3d toroidal$reseatReadback(PhysicsPipeline pipeline, ServerSubLevel subLevel, Pose3d dest, Operation<Pose3d> original) {
        Pose3d readback = original.call(pipeline, subLevel, dest);
        SablePoseFold.reseat((SubLevelPhysicsSystem) (Object) this, subLevel, readback);
        return readback;
    }

    @Inject(method = "onSubLevelRemoved", at = @At("HEAD"))
    private void toroidal$dropConstraintEdges(SubLevel subLevel, SubLevelRemovalReason reason, CallbackInfo callback) {
        if (subLevel instanceof PhysicsPipelineBody body && this.pipeline instanceof SableConstraintGraphs graphs) {
            graphs.toroidal$constraintGraph().dropBody(body);
        }
    }
}
