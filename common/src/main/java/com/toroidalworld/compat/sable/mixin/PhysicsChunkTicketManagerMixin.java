package com.toroidalworld.compat.sable.mixin;

import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.toroidalworld.compat.sable.SableChunkKeys;

import dev.ryanhcode.sable.sublevel.system.ticket.PhysicsChunkTicketManager;

import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

@Mixin(value = PhysicsChunkTicketManager.class, remap = false)
public class PhysicsChunkTicketManagerMixin {
    @WrapMethod(method = "inhabitChunk")
    private void toroidal$inhabitPhysicalChunk(ServerLevel level, DistanceManager distanceManager, UUID subLevelId, long gameTime,
            long chunkLong, int x, int z, Operation<Void> original) {
        ChunkPos physical = SableChunkKeys.physical(level, new ChunkPos(x, z));
        original.call(level, distanceManager, subLevelId, gameTime, physical.toLong(), physical.x, physical.z);
    }
}
