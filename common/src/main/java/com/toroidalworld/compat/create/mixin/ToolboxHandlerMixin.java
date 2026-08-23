package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.equipment.toolbox.ToolboxBlockEntity;
import com.simibubi.create.content.equipment.toolbox.ToolboxHandler;
import com.toroidalworld.compat.create.CreateSeamFold;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

@Mixin(value = ToolboxHandler.class, remap = false)
public class ToolboxHandlerMixin {
    @WrapOperation(method = "withinRange",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/equipment/toolbox/ToolboxHandler;"
                            + "distance(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/core/BlockPos;)D"))
    private static double toroidal$foldToolboxRange(Vec3 location, BlockPos toolboxPos, Operation<Double> original,
            Player player, ToolboxBlockEntity box) {
        BlockPos folded = CreateSeamFold.foldPosition(player.level(), BlockPos.containing(location), toolboxPos);
        return original.call(location, folded);
    }
}
