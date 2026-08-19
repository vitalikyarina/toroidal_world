package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.CompassItemPropertyFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

@Mixin(CompassItemPropertyFunction.class)
public class CompassAngleStateMixin {
    @Unique
    private static final String COMPASS_TARGET_GET =
            "Lnet/minecraft/client/renderer/item/CompassItemPropertyFunction$CompassTarget;"
                    + "getPos(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/world/item/ItemStack;"
                    + "Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/core/GlobalPos;";

    @WrapOperation(method = "getCompassRotation", at = @At(value = "INVOKE", target = COMPASS_TARGET_GET))
    private @Nullable GlobalPos toroidal$needleTargetNearestCopy(
            CompassItemPropertyFunction.CompassTarget compassTarget,
            ClientLevel level, ItemStack itemStack, Entity owner, Operation<GlobalPos> original) {
        GlobalPos target = original.call(compassTarget, level, itemStack, owner);
        if (target == null) {
            return null;
        }

        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedClientBoundsTransformerOf(owner.level());
        if (transformer == null) {
            return target;
        }

        BlockPos stored = target.pos();
        BlockPos nearest = transformer.blocks.nearestCopy(BlockPos.containing(owner.position()), stored);
        return nearest == stored ? target : GlobalPos.of(target.dimension(), nearest);
    }
}
