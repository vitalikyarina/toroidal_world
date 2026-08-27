package com.toroidalworld.compat.sable.mixin;

import java.util.List;
import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.system.ticket.PhysicsChunkTicketManager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

@Mixin(PhysicsChunkTicketManager.class)
public abstract class PhysicsChunkTicketInhabitedMixin {
    @WrapOperation(
            method = "update",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getPlayers(Ljava/util/function/Predicate;)Ljava/util/List;"))
    private List<ServerPlayer> toroidal$inhabitedAcrossSeam(ServerLevel level, Predicate<? super ServerPlayer> inhabits,
            Operation<List<ServerPlayer>> original, @Local(ordinal = 0) BoundingBox3d bounds) {
        WorldFold fold = WorldLoopAttachments.wrappedTransformerOf(level);
        if (fold == null) {
            return original.call(level, inhabits);
        }

        Vec3 centre = new Vec3(
                (bounds.minX() + bounds.maxX()) / 2.0,
                (bounds.minY() + bounds.maxY()) / 2.0,
                (bounds.minZ() + bounds.maxZ()) / 2.0);
        Predicate<ServerPlayer> inhabitsAcrossSeam = player -> {
            Vec3 position = fold.nearestCopy(centre, player.getBoundingBox().getCenter());
            return bounds.contains(position.x, position.y, position.z);
        };
        return original.call(level, inhabitsAcrossSeam);
    }
}
