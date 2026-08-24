package com.toroidalworld.compat.create.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.contraptions.chassis.ChassisBlockEntity;
import com.toroidalworld.compat.create.ChassisWalkFrame;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

@Mixin(value = ChassisBlockEntity.class, remap = false)
public class ChassisBlockEntityMixin {
    @ModifyExpressionValue(
            method = {"addAttachedChasses", "getIncludedBlockPositionsLinear", "getIncludedBlockPositionsRadial"},
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
                    target = "Lcom/simibubi/create/content/contraptions/chassis/ChassisBlockEntity;"
                            + "worldPosition:Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$walkFromTheCallersFrame(BlockPos worldPosition) {
        return ChassisWalkFrame.fold(worldPosition);
    }

    @WrapMethod(method = "collectChassisGroup")
    private List<ChassisBlockEntity> toroidal$collectInTheGroupOriginsFrame(
            Operation<List<ChassisBlockEntity>> original) {
        BlockEntity self = (BlockEntity) (Object) this;
        return ChassisWalkFrame.withAnchor(self.getLevel(), self.getBlockPos(), original::call);
    }
}
