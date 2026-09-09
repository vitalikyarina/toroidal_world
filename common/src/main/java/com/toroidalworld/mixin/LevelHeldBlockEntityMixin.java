package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.client.engine.ClientFrame;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

@Mixin(Level.class)
public class LevelHeldBlockEntityMixin {
    @ModifyReturnValue(method = "getBlockEntity", at = @At("RETURN"))
    private @Nullable BlockEntity toroidal$heldBlockEntity(@Nullable BlockEntity found,
            @Local(argsOnly = true) BlockPos pos) {
        if (found != null) {
            return found;
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level != (Object) this) {
            return null;
        }

        BlockPos held = ClientFrame.heldCopy(pos);
        if (held == null || held.equals(pos)) {
            return null;
        }

        return level.getChunkAt(held).getBlockEntity(held, LevelChunk.EntityCreationType.IMMEDIATE);
    }
}
