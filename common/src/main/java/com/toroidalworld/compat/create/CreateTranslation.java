package com.toroidalworld.compat.create;

import java.util.List;

import com.simibubi.create.content.contraptions.glue.GlueEffectPacket;
import com.simibubi.create.content.equipment.bell.SoulPulseEffectPacket;
import com.simibubi.create.content.equipment.symmetryWand.SymmetryEffectPacket;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmPlacementPacket;
import com.simibubi.create.content.logistics.depot.EjectorPlacementPacket;
import com.simibubi.create.content.logistics.packagePort.PackagePortPlacementPacket;
import com.simibubi.create.content.logistics.packagerLink.WiFiEffectPacket;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterEffectPacket;
import com.simibubi.create.content.logistics.stockTicker.LogisticalStockResponsePacket;
import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.toroidalworld.core.FoldedCopies;
import com.toroidalworld.net.PacketTranslator;
import com.toroidalworld.net.TagPositions;
import com.toroidalworld.net.TranslationContext;

import net.minecraft.core.BlockPos;

public final class CreateTranslation {
    public static final String CONTROLLER_KEY = "Controller";
    public static final String LAST_KNOWN_POS_KEY = "LastKnownPos";
    private static final String PRINTER_KEY = "Printer";
    private static final String ANCHOR_KEY = "Anchor";
    private static final String FLYING_BLOCKS_KEY = "FlyingBlocks";
    private static final String TARGET_KEY = "Target";

    public static void register() {
        if (!CreateMod.present()) {
            return;
        }

        registerSyncedTags();
        registerPlacementEchoes();
        registerEffects();
    }

    private static void registerSyncedTags() {
        SyncedTagFold.register(IMultiBlockEntityContainer.class, TagPositions.PositionShape.BLOCK_POS,
                CONTROLLER_KEY, LAST_KNOWN_POS_KEY);
        SyncedTagFold.registerIn(SchematicannonBlockEntity.class, PRINTER_KEY,
                TagPositions.PositionShape.BLOCK_POS, ANCHOR_KEY);
        SyncedTagFold.registerInEach(SchematicannonBlockEntity.class, FLYING_BLOCKS_KEY,
                TagPositions.PositionShape.BLOCK_POS, TARGET_KEY);
    }

    private static void registerPlacementEchoes() {
        PacketTranslator.registerClientboundPayloadRewriter(ArmPlacementPacket.ClientBoundRequest.class,
                (payload, context) -> {
                    BlockPos placed = seat(context, payload.pos());
                    return placed == payload.pos() ? payload : new ArmPlacementPacket.ClientBoundRequest(placed);
                });

        PacketTranslator.registerClientboundPayloadRewriter(EjectorPlacementPacket.ClientBoundRequest.class,
                (payload, context) -> {
                    BlockPos placed = seat(context, payload.pos());
                    return placed == payload.pos() ? payload : new EjectorPlacementPacket.ClientBoundRequest(placed);
                });

        PacketTranslator.registerClientboundPayloadRewriter(PackagePortPlacementPacket.ClientBoundRequest.class,
                (payload, context) -> {
                    BlockPos placed = seat(context, payload.pos());
                    return placed == payload.pos()
                            ? payload
                            : new PackagePortPlacementPacket.ClientBoundRequest(placed);
                });
    }

    private static void registerEffects() {
        PacketTranslator.registerClientboundPayloadRewriter(SoulPulseEffectPacket.class, (payload, context) -> {
            BlockPos centre = seat(context, payload.pos());
            return centre == payload.pos()
                    ? payload
                    : new SoulPulseEffectPacket(centre, payload.distance(), payload.canOverlap());
        });

        PacketTranslator.registerClientboundPayloadRewriter(WiFiEffectPacket.class, (payload, context) -> {
            BlockPos link = seat(context, payload.pos());
            return link == payload.pos() ? payload : new WiFiEffectPacket(link);
        });

        PacketTranslator.registerClientboundPayloadRewriter(GlueEffectPacket.class, (payload, context) -> {
            BlockPos glued = seat(context, payload.pos());
            return glued == payload.pos()
                    ? payload
                    : new GlueEffectPacket(glued, payload.direction(), payload.fullBlock());
        });

        PacketTranslator.registerClientboundPayloadRewriter(RedstoneRequesterEffectPacket.class,
                (payload, context) -> {
                    BlockPos requester = seat(context, payload.pos());
                    return requester == payload.pos()
                            ? payload
                            : new RedstoneRequesterEffectPacket(requester, payload.success());
                });

        PacketTranslator.registerClientboundPayloadRewriter(SymmetryEffectPacket.class, (payload, context) -> {
            BlockPos mirror = seat(context, payload.mirror());
            List<BlockPos> positions = FoldedCopies.of(payload.positions(),
                    placed -> context.transformer().nearestCopy(mirror, placed));
            return mirror == payload.mirror() && positions == payload.positions()
                    ? payload
                    : new SymmetryEffectPacket(mirror, positions);
        });

        PacketTranslator.registerClientboundPayloadRewriter(LogisticalStockResponsePacket.class,
                (payload, context) -> {
                    BlockPos ticker = seat(context, payload.pos());
                    return ticker == payload.pos()
                            ? payload
                            : new LogisticalStockResponsePacket(payload.lastPacket(), ticker, payload.items());
                });
    }

    private static BlockPos seat(TranslationContext context, BlockPos pos) {
        return context.nearestCopy(pos);
    }

    private CreateTranslation() {
    }
}
