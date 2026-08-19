package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.trains.track.TrackTargetingClient;
import com.toroidalworld.compat.create.client.CreateClientFrame;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;

// The preview drawn on the rail a station, a signal or an observer is about to bind to, and the second reader of the
// same item component the placement gate reads. It takes the stored position straight out of the component, which is
// canonical, and asks the client's own level about it — and the client holds the copy of that ground lying beside the
// player, not the canonical one. With the selection across the seam the block under that name is air there, the
// overlap check answers NO_TRACK, and the preview is dropped before anything is drawn: the player is given no sign of
// what the block will attach to, nor that a selection is live at all.
//
// The other reader, the placement gate, folds against the block being clicked; there is no click here, so this one
// asks the same question of the client's own frame instead.
@Mixin(value = TrackTargetingClient.class, remap = false)
public class TrackTargetingClientMixin {
    @WrapOperation(method = "clientTick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;"))
    private static Object toroidal$foldHoveredTrack(ItemStack stack, DataComponentType<?> component,
            Operation<Object> original) {
        Object value = original.call(stack, component);
        if (component != AllDataComponents.TRACK_TARGETING_ITEM_SELECTED_POS
                || !(value instanceof BlockPos selected)) {
            return value;
        }

        return CreateClientFrame.nearestCopy(Minecraft.getInstance().level, selected);
    }
}
