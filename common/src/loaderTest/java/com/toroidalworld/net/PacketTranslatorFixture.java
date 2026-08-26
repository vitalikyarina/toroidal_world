package com.toroidalworld.net;

import java.util.function.IntFunction;
import java.util.function.IntPredicate;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.player.ClientPosition;
import com.toroidalworld.shape.FlatShape;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

final class PacketTranslatorFixture {
    static final WorldFold TRANSFORMER =
            WorldFolds.of(FlatShape.latticeTorus(new WorldLoopBounds(-32, 32, -32, 32), FlatShape.NO_SKEW));
    static final RegistryAccess.Frozen REGISTRIES =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    static final double MIRROR_X = 580.0;
    static final double MIRROR_Z = -700.0;

    static final BlockPos SERVER_BLOCK = new BlockPos(-510, 64, -505);
    static final BlockPos CLIENT_BLOCK = new BlockPos(514, 64, -505);
    static final BlockPos SERVER_CARRIED_BLOCK = new BlockPos(-510, 64, -100);
    static final BlockPos CLIENT_CARRIED_BLOCK = new BlockPos(514, 64, -100);
    static final BlockPos MIRROR_CARRIED_BLOCK = new BlockPos(514, 64, -1124);
    static final ChunkPos SERVER_CHUNK = new ChunkPos(-32, -32);
    static final ChunkPos CLIENT_CHUNK = new ChunkPos(32, -32);

    static final double SERVER_X = -500.5;
    static final double CLIENT_X = 523.5;
    static final double SERVER_Z = 500.0;
    static final double CLIENT_Z = -524.0;

    static final int VIEW_DISTANCE = 16;

    static final IntFunction<RegistryFriendlyByteBuf> BUFFERS =
            capacity -> new RegistryFriendlyByteBuf(Unpooled.buffer(capacity), REGISTRIES);

    static TranslationContext context() {
        return context(entityId -> false, entityId -> null);
    }

    static TranslationContext context(IntPredicate ownVehicle, IntFunction<Vec3> entityPosition) {
        ClientPosition mirror = new ClientPosition();
        mirror.rebase(MIRROR_X, MIRROR_Z, Level.OVERWORLD, TRANSFORMER);
        return new TranslationContext(TRANSFORMER, mirror, REGISTRIES, BUFFERS, Level.OVERWORLD,
                VIEW_DISTANCE, VIEW_DISTANCE, ownVehicle, entityPosition, () -> {});
    }

    private PacketTranslatorFixture() {
    }
}
