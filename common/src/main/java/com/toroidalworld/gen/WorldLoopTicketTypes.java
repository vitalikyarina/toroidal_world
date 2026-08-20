package com.toroidalworld.gen;

import net.minecraft.server.level.TicketType;

public final class WorldLoopTicketTypes {
    public static final String SEAM_GENERATION_ID = "seam_generation";

    // Loading only: FLAG_SIMULATION would tick the far side and FLAG_PERSIST would write it to the region file.
    public static final TicketType SEAM_GENERATION = new TicketType(TicketType.NO_TIMEOUT, TicketType.FLAG_LOADING);

    private WorldLoopTicketTypes() {
    }
}
