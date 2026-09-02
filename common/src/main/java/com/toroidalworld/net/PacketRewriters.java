package com.toroidalworld.net;

import java.util.Map;
import java.util.function.BiFunction;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.registry.StartupRegistry;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.phys.Vec3;

public final class PacketRewriters {
    public interface ParticleRewriter<P extends ParticleOptions> {
        ParticleOptions rewrite(P particle, TranslationContext context, Vec3 clientOrigin);
    }

    public interface EntityDataRewriter<T> {
        T rewrite(T value, TranslationContext context, Vec3 anchor);
    }

    private final StartupRegistry<Class<?>, BiFunction<CustomPacketPayload, TranslationContext, CustomPacketPayload>>
            clientboundPayloads = new StartupRegistry<>("Clientbound payload rewriters");

    private final StartupRegistry<Class<?>, BiFunction<CustomPacketPayload, TranslationContext, CustomPacketPayload>>
            serverboundPayloads = new StartupRegistry<>("Serverbound payload rewriters");

    private final StartupRegistry<Class<?>, ParticleRewriter<ParticleOptions>> particles =
            new StartupRegistry<>("Particle rewriters");

    private final StartupRegistry<EntityDataSerializer<?>, EntityDataRewriter<Object>> entityData =
            new StartupRegistry<>("Entity data rewriters");

    <P extends CustomPacketPayload> void registerClientboundPayload(Class<P> payloadType,
            BiFunction<P, TranslationContext, CustomPacketPayload> payloadRewriter) {
        clientboundPayloads.register(payloadType, castingRewriter(payloadType, payloadRewriter));
    }

    <P extends CustomPacketPayload> void registerServerboundPayload(Class<P> payloadType,
            BiFunction<P, TranslationContext, CustomPacketPayload> payloadRewriter) {
        serverboundPayloads.register(payloadType, castingRewriter(payloadType, payloadRewriter));
    }

    <P extends ParticleOptions> void registerParticle(Class<P> particleType, ParticleRewriter<P> particleRewriter) {
        particles.register(particleType,
                (particle, context, clientOrigin) -> particleRewriter.rewrite(particleType.cast(particle), context, clientOrigin));
    }

    @SuppressWarnings("unchecked")
    <T> void registerEntityData(EntityDataSerializer<T> serializer, EntityDataRewriter<T> dataRewriter) {
        entityData.register(serializer, (value, context, anchor) -> dataRewriter.rewrite((T) value, context, anchor));
    }

    CustomPacketPayload rewriteClientbound(CustomPacketPayload payload, TranslationContext context) {
        return rewritten(clientboundPayloads.entries(), payload, context);
    }

    CustomPacketPayload rewriteServerbound(CustomPacketPayload payload, TranslationContext context) {
        return rewritten(serverboundPayloads.entries(), payload, context);
    }

    @Nullable ParticleRewriter<ParticleOptions> particleFor(ParticleOptions particle) {
        return particles.entries().get(particle.getClass());
    }

    @Nullable EntityDataRewriter<Object> entityDataFor(SynchedEntityData.DataValue<?> item) {
        return entityData.entries().get(item.serializer());
    }

    private static CustomPacketPayload rewritten(
            Map<Class<?>, BiFunction<CustomPacketPayload, TranslationContext, CustomPacketPayload>> rewriters,
            CustomPacketPayload payload, TranslationContext context) {
        BiFunction<CustomPacketPayload, TranslationContext, CustomPacketPayload> payloadRewriter =
                rewriters.get(payload.getClass());
        return payloadRewriter == null ? payload : payloadRewriter.apply(payload, context);
    }

    private static <P extends CustomPacketPayload> BiFunction<CustomPacketPayload, TranslationContext, CustomPacketPayload>
            castingRewriter(Class<P> payloadType, BiFunction<P, TranslationContext, CustomPacketPayload> payloadRewriter) {
        return (payload, context) -> payloadRewriter.apply(payloadType.cast(payload), context);
    }
}
