package com.toroidalworld.net;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.toroidalworld.core.LogRateGate;

import io.netty.buffer.Unpooled;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.payload.AdvancedAddEntityPayload;

public final class SpawnBufferTranslation {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final LogRateGate WARN_GATE = new LogRateGate();

    private static final String TILE_X_KEY = "TileX";
    private static final String TILE_Y_KEY = "TileY";
    private static final String TILE_Z_KEY = "TileZ";

    public static void register() {
        SpawnBufferFold.register(BlockAttachedEntity.class, TagPositions.PositionShape.BLOCK_INT_TRIPLE,
                TILE_X_KEY, TILE_Y_KEY, TILE_Z_KEY);
        PacketTranslator.registerClientboundPayloadRewriter(
                AdvancedAddEntityPayload.class, SpawnBufferTranslation::seated);
    }

    private static CustomPacketPayload seated(AdvancedAddEntityPayload payload, TranslationContext context) {
        Class<?> entityClass = context.entityClass().apply(payload.entityId());
        if (entityClass == null || !SpawnBufferFold.carriesPositions(entityClass)) {
            return payload;
        }

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(payload.customPayload()));
        CompoundTag spawnData;
        try {
            spawnData = buffer.readNbt();
        } catch (RuntimeException malformed) {
            warnBufferDoesNotOpenWithACompound(entityClass, malformed);
            return payload;
        }

        if (spawnData == null) {
            return payload;
        }

        CompoundTag seated = SpawnBufferFold.seatedIn(seatIn(context), entityClass, spawnData);
        if (seated == spawnData) {
            return payload;
        }

        FriendlyByteBuf rewritten = new FriendlyByteBuf(Unpooled.buffer(payload.customPayload().length));
        rewritten.writeNbt(seated);
        rewritten.writeBytes(buffer);

        byte[] bytes = new byte[rewritten.readableBytes()];
        rewritten.readBytes(bytes);
        return new AdvancedAddEntityPayload(payload.entityId(), bytes);
    }

    private static TagPositions.Seat seatIn(TranslationContext context) {
        return new TagPositions.Seat() {
            @Override
            public BlockPos seat(BlockPos stored) {
                return context.nearestCopy(stored);
            }

            @Override
            public Vec3 seat(Vec3 stored) {
                return context.nearestCopy(stored);
            }
        };
    }

    private static void warnBufferDoesNotOpenWithACompound(Class<?> entityClass, RuntimeException malformed) {
        if (WARN_GATE.tryPass()) {
            LOGGER.warn("The spawn buffer of {} is registered as carrying world positions in NBT but does not open"
                            + " with a compound, so its coordinates reach the client unseated",
                    entityClass.getName(), malformed);
        }
    }

    private SpawnBufferTranslation() {
    }
}
