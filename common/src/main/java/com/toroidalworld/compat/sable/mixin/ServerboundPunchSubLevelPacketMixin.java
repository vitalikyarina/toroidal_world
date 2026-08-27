package com.toroidalworld.compat.sable.mixin;

import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.toroidalworld.compat.sable.SeamFrame;

import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.network.packets.tcp.ServerboundPunchSubLevelPacket;

import net.minecraft.world.entity.player.Player;

@Mixin(ServerboundPunchSubLevelPacket.class)
public abstract class ServerboundPunchSubLevelPacketMixin {
    private static final String PLAYER_INVERSE = "Ldev/ryanhcode/sable/companion/math/Pose3d;transformPositionInverse(Lorg/joml/Vector3d;)Lorg/joml/Vector3d;";

    @WrapOperation(
            method = "handle",
            at = {
                    @At(value = "INVOKE", target = PLAYER_INVERSE, ordinal = 1),
                    @At(value = "INVOKE", target = PLAYER_INVERSE, ordinal = 2)
            })
    private Vector3d toroidal$frameOnPunch(Pose3d pose, Vector3d global, Operation<Vector3d> original, @Local Player player) {
        return SeamFrame.with(player.level(), player::position, () -> original.call(pose, global));
    }
}
