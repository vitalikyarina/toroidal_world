package com.toroidalworld.compat.aeronautics;

import java.util.Map;

import org.joml.Vector3d;
import org.jspecify.annotations.Nullable;

import com.toroidalworld.compat.aeronautics.mixin.MultiMiningSyncAccessor;
import com.toroidalworld.compat.aeronautics.mixin.PhysicsStaffBeamPacketAccessor;
import com.toroidalworld.compat.create.SyncedTagFold;
import com.toroidalworld.core.FoldedCopies;
import com.toroidalworld.net.PacketTranslator;
import com.toroidalworld.net.SpawnBufferFold;
import com.toroidalworld.net.TagPositions;
import com.toroidalworld.net.TranslationContext;

import dev.eriksonn.aeronautics.network.packets.LevititeCatalystCrystallizationPacket;
import dev.simulated_team.simulated.content.blocks.docking_connector.DockingConnectorBlockEntity;
import dev.simulated_team.simulated.content.blocks.lasers.laser_pointer.LaserPointerBlockEntity;
import dev.simulated_team.simulated.content.blocks.merging_glue.MergingGlueBlockEntity;
import dev.simulated_team.simulated.content.blocks.nameplate.NameplateBlockEntity;
import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity;
import dev.simulated_team.simulated.content.blocks.spring.SpringBlockEntity;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.SwivelBearingBlockEntity;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.link_block.SwivelBearingPlateBlockEntity;
import dev.simulated_team.simulated.content.entities.honey_glue.HoneyGlueEntity;
import dev.simulated_team.simulated.index.SimEntityDataSerializers;
import dev.simulated_team.simulated.network.packets.honey_glue.HoneyGlueSyncBoundsPacket;
import dev.simulated_team.simulated.network.packets.linked_typewriter.TypewriterKeySavePacket;
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
    private static final String SPRING_GOAL_KEY = "Goal";
    private static final String MERGING_GLUE_PARTNER_KEY = "PartnerPosition";
    private static final String DOCKING_OTHER_CONNECTOR_KEY = "OtherConnector";
    private static final String NAMEPLATE_CONTROLLER_KEY = "ControllerPos";
    private static final String SWIVEL_PLATE_KEY = "SwivelPlate";
    private static final String SWIVEL_PARENT_KEY = "ParentPos";
    private static final String NAV_TARGET_KEY = "CurrentTarget";
    private static final String LASER_HIT_KEY = "HitPos";
    private static final String HONEY_GLUE_POS_KEY = "Pos";

    public static void register() {
        if (SimulatedMod.present()) {
            registerSimulated();
        }

        if (OffroadMod.present()) {
            registerOffroad();
        }

        if (AeronauticsMod.present()) {
            registerAeronautics();
        }
    }

    private static void registerAeronautics() {
        PacketTranslator.registerServerboundPayloadRewriter(LevititeCatalystCrystallizationPacket.class,
                (payload, context) ->
                        new LevititeCatalystCrystallizationPacket(context.toServer(payload.pos()), payload.hand()));
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
        registerSimulatedSyncedTags();
        SpawnBufferFold.register(HoneyGlueEntity.class, TagPositions.PositionShape.VEC3_LIST, HONEY_GLUE_POS_KEY);

        PacketTranslator.registerServerboundPayloadRewriter(TypewriterKeySavePacket.class, (payload, context) ->
                new TypewriterKeySavePacket(payload.changedKeys(), context.toServer(payload.pos()), payload.clearAll()));

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

    private static void registerSimulatedSyncedTags() {
        SyncedTagFold.register(SpringBlockEntity.class, TagPositions.PositionShape.PACKED_LONG, SPRING_GOAL_KEY);
        SyncedTagFold.register(MergingGlueBlockEntity.class, TagPositions.PositionShape.PACKED_LONG,
                MERGING_GLUE_PARTNER_KEY);
        SyncedTagFold.register(DockingConnectorBlockEntity.class, TagPositions.PositionShape.BLOCK_POS,
                DOCKING_OTHER_CONNECTOR_KEY);
        SyncedTagFold.register(NameplateBlockEntity.class, TagPositions.PositionShape.BLOCK_POS,
                NAMEPLATE_CONTROLLER_KEY);
        SyncedTagFold.register(SwivelBearingBlockEntity.class, TagPositions.PositionShape.BLOCK_POS,
                SWIVEL_PLATE_KEY);
        SyncedTagFold.register(SwivelBearingPlateBlockEntity.class, TagPositions.PositionShape.BLOCK_POS,
                SWIVEL_PARENT_KEY);
        SyncedTagFold.register(NavTableBlockEntity.class, TagPositions.PositionShape.VEC3_LIST, NAV_TARGET_KEY);
        SyncedTagFold.register(LaserPointerBlockEntity.class, TagPositions.PositionShape.VEC3_LIST, LASER_HIT_KEY);
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
