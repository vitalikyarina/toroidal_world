package com.toroidalworld.compat.aeronautics.mixin;

import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.toroidalworld.compat.aeronautics.RopeSeamFrame;

import dev.simulated_team.simulated.content.blocks.rope.strand.server.RopeAttachment;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.ServerRopeStrand;

import net.minecraft.server.level.ServerLevel;

@Mixin(value = ServerRopeStrand.class, remap = false)
public class ServerRopeStrandMixin {
    @ModifyExpressionValue(
            method = "applyAttachment",
            at = @At(value = "INVOKE",
                    target = "Ldev/ryanhcode/sable/companion/math/JOMLConversion;toJOML("
                            + "Lnet/minecraft/core/Position;)Lorg/joml/Vector3d;"))
    private Vector3d toroidal$seatAttachmentPin(Vector3d attachmentPoint,
            @Local(argsOnly = true) RopeAttachment attachment, @Local(argsOnly = true) ServerLevel level) {
        return RopeSeamFrame.seatAttachment((ServerRopeStrand) (Object) this, attachment, level, attachmentPoint);
    }
}
