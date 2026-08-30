package com.toroidalworld.compat.aeronautics.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.toroidalworld.compat.aeronautics.MergingGlueSeamFrame;

import dev.simulated_team.simulated.content.blocks.merging_glue.MergingGlueBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntity;

@Mixin(value = MergingGlueBlockEntity.class, remap = false)
public class MergingGlueBlockEntityMixin {
    @WrapMethod(method = "startControlling")
    private void toroidal$controlInTheSeamFrame(MergingGlueBlockEntity partner, Operation<Void> original) {
        MergingGlueSeamFrame.control((BlockEntity) (Object) this, partner, () -> original.call(partner));
    }
}
