package com.toroidalworld.compat.sable.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.toroidalworld.compat.sable.SeamFrame;

import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplFrameMixin {
    @Shadow
    public ServerPlayer player;

    @WrapMethod(method = "handleMovePlayer")
    private void toroidal$frameOnMove(ServerboundMovePlayerPacket packet, Operation<Void> original) {
        framed(() -> original.call(packet));
    }

    @WrapMethod(method = "handleMoveVehicle")
    private void toroidal$frameOnVehicleMove(ServerboundMoveVehiclePacket packet, Operation<Void> original) {
        framed(() -> original.call(packet));
    }

    @WrapMethod(method = "handlePlayerAction")
    private void toroidal$frameOnPlayerAction(ServerboundPlayerActionPacket packet, Operation<Void> original) {
        framed(() -> original.call(packet));
    }

    @WrapMethod(method = "handleUseItemOn")
    private void toroidal$frameOnUseItemOn(ServerboundUseItemOnPacket packet, Operation<Void> original) {
        framed(() -> original.call(packet));
    }

    @WrapMethod(method = "handleUseItem")
    private void toroidal$frameOnUseItem(ServerboundUseItemPacket packet, Operation<Void> original) {
        framed(() -> original.call(packet));
    }

    @WrapMethod(method = "handleInteract")
    private void toroidal$frameOnInteract(ServerboundInteractPacket packet, Operation<Void> original) {
        framed(() -> original.call(packet));
    }

    private void framed(Runnable body) {
        SeamFrame.run(this.player.serverLevel(), this.player::position, body);
    }
}
