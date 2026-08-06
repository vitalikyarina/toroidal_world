package com.toroidalworld.gen;

import java.util.function.Supplier;

import com.toroidalworld.ToroidalWorld;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.TicketType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class WorldLoopTicketTypes {
    public static final String SEAM_GENERATION_ID = "seam_generation";

    public static final DeferredRegister<TicketType> TICKET_TYPES =
            DeferredRegister.create(Registries.TICKET_TYPE, ToroidalWorld.MODID);

    // Loading only, and deliberately nothing else. FLAG_SIMULATION would make the far side tick; FLAG_PERSIST would
    // write it to the region file, so a ticket that is meant to live for one generation pass would outlive the session.
    // No timeout either: canTicketExpire never expires a timeout-less type, so the ticket goes exactly when its owner
    // releases it — and if a release were ever missed, deactivateTicketsOnClosing still clears it when the level closes.
    public static final Supplier<TicketType> SEAM_GENERATION =
            TICKET_TYPES.register(SEAM_GENERATION_ID, () -> new TicketType(TicketType.NO_TIMEOUT, TicketType.FLAG_LOADING));

    private WorldLoopTicketTypes() {
    }
}
