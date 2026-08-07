package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.server.commands.SpreadPlayersCommand;

// A spread position is two numbers and nothing else — no level, no bounds — so the seam-aware arithmetic cannot live on
// it and has to reach in from the command instead. Everything else the algorithm needs (randomize, isSafe) is already
// public on the class; only the two coordinates are not.
@Mixin(SpreadPlayersCommand.Position.class)
public interface SpreadPositionAccessor {
    @Accessor("x")
    double toroidal$x();

    @Accessor("x")
    void toroidal$setX(double x);

    @Accessor("z")
    double toroidal$z();

    @Accessor("z")
    void toroidal$setZ(double z);
}
