package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.toroidalworld.compat.create.client.CreateMenuFrame;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

@Mixin(FriendlyByteBuf.class)
public class FriendlyByteBufMenuPositionMixin {
    // BlockPos.STREAM_CODEC decodes through this static, so narrowing the target to the instance readBlockPos
    // would leave every codec-carried position in a Create menu payload unfolded and silent.
    @ModifyReturnValue(
            method = "readBlockPos(Lio/netty/buffer/ByteBuf;)Lnet/minecraft/core/BlockPos;",
            at = @At("RETURN"))
    private static BlockPos toroidal$foldMenuPayloadPosition(BlockPos canonical) {
        return CreateMenuFrame.fold(canonical);
    }
}
