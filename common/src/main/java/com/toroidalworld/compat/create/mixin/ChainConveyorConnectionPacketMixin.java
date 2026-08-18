package com.toroidalworld.compat.create.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorConnectionPacket;
import com.toroidalworld.compat.create.CreateSeamFold;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

// Where a chain conveyor connection across the seam is actually refused. Both coordinates the client sends are its own
// hit results, so they leave the client already the short way apart; what parts them is the server. The packet's own
// position goes through isLoaded, canInteractWithBlock and getBlockEntity, all three of which this mod folds, so the
// block entity resolves and its getBlockPos() answers canonical — while the other coordinate is never asked anything
// that would rename it and stays in the client's frame. Which of the two carries that frame depends on the click order,
// and so does the way it fails: click the far conveyor first and the range gate measures a canonical position against a
// client one, reads a world and drops the packet in silence; click the near one first and the gate passes, but the far
// conveyor is then handed a canonical position a world from its own and stores that as its offset, so the two ends
// disagree and the connection is swept away as invalid. The player has seen the green line, the valid_connection status
// and heard the chain go down, and nothing is built either way.
//
// So both coordinates are folded, onto the block entity: it is the subject every arithmetic in the method is relative
// to and the one anchor that travels with the state being written. That single reframing answers the whole method —
// the range gate, the block entity lookup for the far end, the chain charged on connect and the chain refunded on
// disconnect, the destroyed-chain effect, and the two positions handed to addConnectionTo and removeConnectionTo.
//
// The two cost deltas are worth naming, because they are one subtraction only while the two frames agree: the charge is
// measured from the block entity, the refund from the packet's own position. Folding both fields onto the same anchor is
// what makes them the same delta again.
//
// Server-side by construction — applySettings runs from handle(ServerPlayer), so the level's own transformer is the
// truth here. Neither field is rewritten anywhere: the packet is read once and discarded.
@Mixin(value = ChainConveyorConnectionPacket.class, remap = false)
public class ChainConveyorConnectionPacketMixin {
    @ModifyExpressionValue(
            method = "applySettings",
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
                    target = "Lcom/simibubi/create/content/kinetics/chainConveyor/ChainConveyorConnectionPacket;targetPos:Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$foldTargetPos(BlockPos targetPos, ServerPlayer player, ChainConveyorBlockEntity be) {
        return CreateSeamFold.foldPosition(be.getLevel(), be.getBlockPos(), targetPos);
    }

    // The packet's own position names the same block the block entity does, so folding it onto that block entity is what
    // normalises it back to the frame getBlockPos() answers in. Its one arithmetic use is the disconnect refund.
    @ModifyExpressionValue(
            method = "applySettings",
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
                    target = "Lcom/simibubi/create/content/kinetics/chainConveyor/ChainConveyorConnectionPacket;pos:Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$foldPacketPos(BlockPos pos, ServerPlayer player, ChainConveyorBlockEntity be) {
        return CreateSeamFold.foldPosition(be.getLevel(), be.getBlockPos(), pos);
    }
}
