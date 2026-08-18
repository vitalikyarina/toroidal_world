package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.kinetics.belt.item.BeltConnectorHandler;
import com.toroidalworld.compat.create.CreateSeamFold;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;

// The dotted line the connector draws between the pulley already clicked and the block under the crosshair — the only
// thing Create tells the player about whether a belt will connect. It reads the same stored component the server does,
// measures it against the block being looked at, and returns before drawing anything when that reads farther than the
// belt may be.
//
// What breaks here is not the seam but a lap. The client's space is continuous, so a pair straddling the seam is drawn
// adjacent and measures adjacent; but the component holds an absolute server coordinate, and once the player's own
// unbounded coordinate has wound a whole world width away, that coordinate names a block a world from the one they are
// looking at. The gate then refuses and the player gets no line at all — not a red one saying no, nothing, and
// everywhere rather than only near the seam.
//
// The same single fold as the server's, on the client's own terms. Two things differ. The client level's transformer is
// NOOP by design, so the bounds that answer are the ones the server sent. And the anchor is the player rather than the
// block being looked at: the component is read before the ray trace, and the player is within a few blocks of whatever
// they are pointing at — far closer than the half world that would be needed to fold the two differently.
//
// Folding it here is what makes the rest of the method work in one step, because everything after the read is derived
// from that position: the length gate, canConnect asked of the client level, the two corner vectors the particle line
// is interpolated between, and the axis test that decides its direction.
@Mixin(value = BeltConnectorHandler.class, remap = false)
public class BeltConnectorHandlerMixin {
    @ModifyExpressionValue(
            method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;"))
    private static Object toroidal$foldStoredPulley(Object stored) {
        if (!(stored instanceof BlockPos storedPulley)) {
            return stored;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) {
            return stored;
        }

        return CreateSeamFold.foldClientPosition(level, player.blockPosition(), storedPulley);
    }
}
