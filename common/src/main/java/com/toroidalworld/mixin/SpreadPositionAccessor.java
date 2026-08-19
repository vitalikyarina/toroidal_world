package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.server.commands.SpreadPlayersCommand;

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
