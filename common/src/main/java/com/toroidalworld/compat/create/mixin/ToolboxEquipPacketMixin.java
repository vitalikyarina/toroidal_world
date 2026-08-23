package com.toroidalworld.compat.create.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.equipment.toolbox.ToolboxEquipPacket;
import com.toroidalworld.compat.create.CreateEquipmentFold;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

@Mixin(value = ToolboxEquipPacket.class, remap = false)
public class ToolboxEquipPacketMixin {
    @Mutable
    @Shadow
    @Final
    private @Nullable BlockPos toolboxPos;

    @Inject(method = "handle", at = @At("HEAD"))
    private void toroidal$canonicaliseToolboxPos(ServerPlayer player, CallbackInfo ci) {
        toolboxPos = CreateEquipmentFold.canonicalisePacketPosition(player.level(), toolboxPos);
    }
}
