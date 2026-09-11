package com.toroidalworld.compat.sable;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;

record SableMemberPose(PhysicsPipelineBody body, @Nullable Pose3dc pose) {
    static List<SableMemberPose> substep(PhysicsPipeline pipeline, List<PhysicsPipelineBody> group,
            ServerSubLevel self, Pose3d readback) {
        List<SableMemberPose> members = new ArrayList<>(group.size());
        for (PhysicsPipelineBody body : group) {
            if (body == self) {
                members.add(new SableMemberPose(body, readback));
            } else if (body instanceof ServerSubLevel subLevel) {
                members.add(new SableMemberPose(body, pipeline.readPose(subLevel, new Pose3d())));
            } else {
                members.add(new SableMemberPose(body, SableBodyPose.of(body)));
            }
        }

        return members;
    }

    static List<SableMemberPose> logical(List<PhysicsPipelineBody> group) {
        List<SableMemberPose> members = new ArrayList<>(group.size());
        for (PhysicsPipelineBody body : group) {
            members.add(new SableMemberPose(body, SableBodyPose.of(body)));
        }

        return members;
    }

    static List<PhysicsPipelineBody> bodies(List<SableMemberPose> members) {
        List<PhysicsPipelineBody> bodies = new ArrayList<>(members.size());
        for (SableMemberPose member : members) {
            bodies.add(member.body());
        }

        return bodies;
    }
}
