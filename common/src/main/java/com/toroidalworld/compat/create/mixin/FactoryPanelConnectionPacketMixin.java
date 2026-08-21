package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnectionPacket;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import com.toroidalworld.compat.create.CreateFactoryPanelFold;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

@Mixin(value = FactoryPanelConnectionPacket.class, remap = false)
public class FactoryPanelConnectionPacketMixin {
    @Mutable
    @Shadow
    @Final
    private FactoryPanelPosition fromPos;

    @Mutable
    @Shadow
    @Final
    private FactoryPanelPosition toPos;

    @Inject(method = "applySettings(Lnet/minecraft/server/level/ServerPlayer;"
            + "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBlockEntity;)V",
            at = @At("HEAD"))
    private void toroidal$canonicalisePanelPositions(ServerPlayer player, FactoryPanelBlockEntity blockEntity,
            CallbackInfo ci) {
        Level level = player.level();
        this.fromPos = CreateFactoryPanelFold.canonical(level, this.fromPos);
        this.toPos = CreateFactoryPanelFold.canonical(level, this.toPos);
    }
}
