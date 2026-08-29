package com.toroidalworld.compat.aeronautics.mixin;

import java.util.HashMap;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.toroidalworld.compat.aeronautics.MagnetSeamFrame;

import dev.simulated_team.simulated.content.blocks.redstone_magnet.RedstoneMagnetBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntity;

@Mixin(value = RedstoneMagnetBlockEntity.class, remap = false)
public class RedstoneMagnetBlockEntityMixin {
    @WrapOperation(
            method = "spawnParticles",
            at = @At(value = "INVOKE", target = "Ljava/util/HashMap;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object toroidal$seatFieldSource(HashMap<Object, Object> positions, Object nearby, Object moment,
            Operation<Object> original) {
        return original.call(positions, MagnetSeamFrame.seatNearbyMagnet((BlockEntity) (Object) this, nearby), moment);
    }
}
