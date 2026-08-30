package com.toroidalworld.compat.aeronautics.mixin;

import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.sugar.Local;
import com.toroidalworld.compat.aeronautics.SpringSeamFrame;

import dev.simulated_team.simulated.content.blocks.spring.SpringBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntity;

@Mixin(value = SpringBlockEntity.class, remap = false)
public class SpringBlockEntityMixin {
    private static final int OWN_POSITION_SLOT = 15;
    private static final int PARTNER_POSITION_SLOT = 16;

    @ModifyVariable(method = "sable$physicsTick", at = @At("STORE"), index = PARTNER_POSITION_SLOT)
    private Vector3d toroidal$seatPartnerInOwnFrame(Vector3d partnerPosition,
            @Local(index = OWN_POSITION_SLOT) Vector3d ownPosition) {
        return SpringSeamFrame.seat(((BlockEntity) (Object) this).getLevel(), ownPosition, partnerPosition);
    }
}
