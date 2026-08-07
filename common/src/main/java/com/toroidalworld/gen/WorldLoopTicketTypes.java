package com.toroidalworld.gen;

import net.minecraft.server.level.TicketType;

public final class WorldLoopTicketTypes {
    public static final String SEAM_GENERATION_ID = "seam_generation";

    // Loading only, and deliberately nothing else. FLAG_SIMULATION would make the far side tick; FLAG_PERSIST would
    // write it to the region file, so a ticket that is meant to live for one generation pass would outlive the session.
    // No timeout either: canTicketExpire never expires a timeout-less type, so the ticket goes exactly when its owner
    // releases it — and if a release were ever missed, deactivateTicketsOnClosing still clears it when the level closes.
    //
    // The instance is created here, loader-free; registration under SEAM_GENERATION_ID is loader glue in WorldLoop,
    // which registers this very instance.
    public static final TicketType SEAM_GENERATION = new TicketType(TicketType.NO_TIMEOUT, TicketType.FLAG_LOADING);

    private WorldLoopTicketTypes() {
    }
}
