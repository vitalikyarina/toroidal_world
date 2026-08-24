package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.logistics.packagePort.PackagePortPlacementPacket;
import com.toroidalworld.compat.create.CreateSeamFold;
import com.toroidalworld.compat.create.CreateTrackFold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

@Mixin(value = PackagePortPlacementPacket.class, remap = false)
public class PackagePortPlacementPacketMixin {
    @Mutable
    @Shadow
    @Final
    private BlockPos pos;

    @Inject(method = "handle", at = @At("HEAD"))
    private void toroidal$canonicalisePortPos(ServerPlayer player, CallbackInfo ci) {
        if (player != null) {
            this.pos = CreateSeamFold.canonical(player.serverLevel(), this.pos);
        }
    }

    @WrapOperation(method = "handle",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;closerThan(Lnet/minecraft/core/Position;D)Z"))
    private boolean toroidal$foldPortRange(Vec3 target, Position port, double range, Operation<Boolean> original,
            ServerPlayer player) {
        if (player == null || !(port instanceof Vec3 anchor)) {
            return original.call(target, port, range);
        }

        return original.call(CreateTrackFold.nearestCopy(player.level(), anchor, target), port, range);
    }
}
