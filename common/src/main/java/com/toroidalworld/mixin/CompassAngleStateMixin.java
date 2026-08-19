package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.CompassAngleState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;

@Mixin(CompassAngleState.class)
public class CompassAngleStateMixin {
    @WrapOperation(
            method = "calculate",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/item/properties/numeric/CompassAngleState$CompassTarget;"
                            + "get(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/world/item/ItemStack;"
                            + "Lnet/minecraft/world/entity/ItemOwner;)Lnet/minecraft/core/GlobalPos;"))
    private @Nullable GlobalPos toroidal$needleTargetNearestCopy(CompassAngleState.CompassTarget compassTarget,
            ClientLevel level, ItemStack itemStack, ItemOwner owner, Operation<GlobalPos> original) {
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
