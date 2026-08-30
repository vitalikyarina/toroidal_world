package com.toroidalworld.compat.aeronautics.mixin;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.toroidalworld.compat.aeronautics.LinkedReceiverSeamDelta;

import dev.simulated_team.simulated.content.blocks.redstone.AbstractLinkedReceiverBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntity;

@Mixin(value = AbstractLinkedReceiverBlockEntity.class, remap = false)
public class AbstractLinkedReceiverBlockEntityMixin {
    @WrapOperation(method = "updateSignal",
            at = @At(value = "INVOKE",
                    target = "Lorg/joml/Vector3d;sub(Lorg/joml/Vector3dc;)Lorg/joml/Vector3d;"))
    private Vector3d toroidal$foldRelativePosition(Vector3d target, Vector3dc current,
            Operation<Vector3d> original) {
        return LinkedReceiverSeamDelta.fold((BlockEntity) (Object) this, target, current, original);
    }
}
