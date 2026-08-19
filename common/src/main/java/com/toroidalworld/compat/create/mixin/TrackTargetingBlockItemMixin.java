package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.trains.track.TrackTargetingBlockItem;
import com.toroidalworld.compat.create.CreateTrackFold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

// Binding a station, a signal or an observer to a piece of track takes two clicks, and the first is kept in an item
// component until the second arrives. Both blocks are canonical, each canonicalized on its own click, so a pair
// straddling the seam reads about a world apart — and the first thing the second click does with them is measure the
// gap against 16 blocks. The placement is refused outright, straight track and curve alike, and every line of geometry
// behind that gate never runs. Both sides refuse, because the measurement sits ahead of each isClientSide guard in
// that branch.
//
// So the fold goes on the stored click, taken at the one read that produces it. The gate and the delta the block
// entity stores are the same local, so a single statement answers both: the gate measures a few blocks, and
// TargetTrack names the short way round instead of a world.
//
// That delta is not what resolves the rail — a raw subtraction added back to the placed block already names the
// selected one exactly — it is what everything downstream reads as geometry. The station derives its bogey offset
// from it, the render bounds are drawn from it, and the renderers resolve the rail they overlay through it on the
// client, which has the copy beside the seam loaded and not the canonical one half a world away. The resolved
// position then runs a little past the bounds, which is what each of those wants: they reach the world through Level,
// which wraps what it is handed, and the graph folds its own node keys.
//
// The Bezier key is deliberately left alone. curveTarget is not a position but the key of the track block entity's
// connection map, which BezierConnectionMixin keeps canonical on purpose, and the raw subtraction is exactly what
// reproduces it once the block entity adds its own position back. Folding it would hand the lookup a different copy
// and the curve would never be found.
//
// The anchor is the clicked block rather than the block being placed. The two are one step apart in the same frame,
// so they choose the same copy of anything the 16-block gate would let through.
@Mixin(value = TrackTargetingBlockItem.class, remap = false)
public class TrackTargetingBlockItemMixin {
    @WrapOperation(method = "useOn",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;"))
    private Object toroidal$foldSelectedTrack(ItemStack stack, DataComponentType<?> component,
            Operation<Object> original, UseOnContext context) {
        Object value = original.call(stack, component);
        if (component != AllDataComponents.TRACK_TARGETING_ITEM_SELECTED_POS
                || !(value instanceof BlockPos selected)) {
            return value;
        }

        return CreateTrackFold.nearestCopy(context.getLevel(), context.getClickedPos(), selected);
    }
}
