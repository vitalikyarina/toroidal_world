package com.toroidalworld.compat.sable.client;

import org.joml.Vector3d;
import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class SableClientFrame {
    public static void reseat(@Nullable Level level, Pose3dc pose) {
        if (level == null || !(pose instanceof Pose3d received)) {
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        WorldFold fold = WorldLoopAttachments.wrappedClientBoundsTransformerOf(level);
        if (player == null || fold == null) {
            return;
        }

        Vector3d position = received.position();
        Vec3 raw = new Vec3(position.x, position.y, position.z);
        Vec3 seated = fold.nearestCopy(player.position(), raw);
        if (seated == raw) {
            return;
        }

        position.set(seated.x, seated.y, seated.z);
    }

    private SableClientFrame() {
    }
}
