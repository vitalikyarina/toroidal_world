package com.toroidalworld.compat.aeronautics;

import java.util.List;
import java.util.Map;

import org.joml.Vector3d;
import org.jspecify.annotations.Nullable;

import com.toroidalworld.compat.aeronautics.mixin.MultiMiningSyncAccessor;
import com.toroidalworld.compat.aeronautics.mixin.PhysicsStaffBeamPacketAccessor;
import com.toroidalworld.core.FoldedCopies;
import com.toroidalworld.net.PacketTranslator;
import com.toroidalworld.net.TranslationContext;

import dev.simulated_team.simulated.index.SimEntityDataSerializers;
import dev.simulated_team.simulated.network.packets.honey_glue.HoneyGlueSyncBoundsPacket;
import dev.simulated_team.simulated.network.packets.lodestone_compass.UpdateClientLodestonePositionPacket;
import dev.simulated_team.simulated.network.packets.physics_assembler.PhysicsAssemblerFailedPacket;
import dev.simulated_team.simulated.network.packets.physics_assembler.PhysicsAssemblerFlickAndHoldLeverPacket;
import dev.ryanhcode.offroad.handlers.server.MultiMiningServerManager;
import dev.ryanhcode.offroad.network.borehead_bearing.ClientboundMultiMiningSync;
import dev.simulated_team.simulated.network.packets.physics_staff.PhysicsStaffBeamPacket;
import dev.simulated_team.simulated.network.packets.physics_staff.PhysicsStaffDragSessionsPacket;
import dev.simulated_team.simulated.network.packets.rope.ClientboundRopeDataPacket;
import dev.simulated_team.simulated.network.packets.rope.ClientboundRopeStoppedPacket;

import net.createmod.catnip.data.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class AeronauticsTranslation {
    public static void register() {
        if (SimulatedMod.present()) {
            registerSimulated();
        }

        if (OffroadMod.present()) {
            registerOffroad();
        }
    }

    private static void registerOffroad() {
        PacketTranslator.registerClientboundPayloadRewriter(ClientboundMultiMiningSync.class, (payload, context) -> {
            ClientboundMultiMiningSync seated = ClientboundMultiMiningSync.serverOutboundData(
                    ((MultiMiningSyncAccessor) (Object) payload).toroidal$breakingId());
            for (Map.Entry<BlockPos, MultiMiningServerManager.BlockBreakingData> entry : payload.inData.entrySet()) {
                seated.inData.put(seat(context, entry.getKey()), entry.getValue());
            }

            return seated;
        });
    }

    private static void registerSimulated() {

        PacketTranslator.registerClientboundPayloadRewriter(ClientboundRopeDataPacket.class, (payload, context) ->
                new ClientboundRopeDataPacket(payload.interpolationTick(), seat(context, payload.ownerPos()),
                        payload.uuid(), FoldedCopies.of(payload.points(), point -> seat(context, point)),
                        seat(context, payload.startAttachmentPos()), seat(context, payload.endAttachmentPos())));

        PacketTranslator.registerClientboundPayloadRewriter(ClientboundRopeStoppedPacket.class, (payload, context) ->
                new ClientboundRopeStoppedPacket(seat(context, payload.ownerPos())));

        PacketTranslator.registerClientboundPayloadRewriter(HoneyGlueSyncBoundsPacket.class, (payload, context) ->
                new HoneyGlueSyncBoundsPacket(seat(context, payload.bounds()), payload.honeyGlueId(), payload.uuid()));

        PacketTranslator.registerClientboundPayloadRewriter(PhysicsStaffDragSessionsPacket.class, (payload, context) ->
                new PhysicsStaffDragSessionsPacket(payload.dimension(),
                        FoldedCopies.of(payload.sessions(), session ->
                                Pair.of(session.getFirst(), seat(context, session.getSecond())))));

        PacketTranslator.registerClientboundPayloadRewriter(PhysicsAssemblerFailedPacket.class, (payload, context) ->
                new PhysicsAssemblerFailedPacket(seat(context, payload.pos())));

        PacketTranslator.registerClientboundPayloadRewriter(PhysicsAssemblerFlickAndHoldLeverPacket.class, (payload, context) ->
                new PhysicsAssemblerFlickAndHoldLeverPacket(seat(context, payload.pos()), payload.flicked()));

        PacketTranslator.registerClientboundPayloadRewriter(UpdateClientLodestonePositionPacket.class, (payload, context) ->
                new UpdateClientLodestonePositionPacket(payload.id(), seat(context, payload.sentPosition())));

        PacketTranslator.registerClientboundPayloadRewriter(PhysicsStaffBeamPacket.class, (payload, context) -> {
            PhysicsStaffBeamPacketAccessor beam = (PhysicsStaffBeamPacketAccessor) payload;
            return new PhysicsStaffBeamPacket(beam.toroidal$uuid(), seat(context, beam.toroidal$start()),
                    seat(context, beam.toroidal$end()));
        });

        PacketTranslator.registerEntityDataRewriter(SimEntityDataSerializers.VEC3,
                (position, context, anchor) -> context.transformer().nearestCopy(anchor, position));
    }

    private static @Nullable BlockPos seat(TranslationContext context, @Nullable BlockPos pos) {
        return pos == null ? null : context.nearestCopy(pos);
    }

    private static Vector3d seat(TranslationContext context, Vector3d point) {
        Vec3 seated = context.nearestCopy(new Vec3(point.x, point.y, point.z));
        return seated.x == point.x && seated.z == point.z ? point : new Vector3d(seated.x, seated.y, seated.z);
    }

    private static AABB seat(TranslationContext context, AABB bounds) {
        Vec3 corner = new Vec3(bounds.minX, bounds.minY, bounds.minZ);
        Vec3 seated = context.nearestCopy(corner);
        return seated == corner ? bounds : bounds.move(seated.x - corner.x, 0.0, seated.z - corner.z);
    }

    private AeronauticsTranslation() {
    }
}
