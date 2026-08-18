package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.kinetics.belt.item.BeltConnectorItem;
import com.toroidalworld.compat.create.CreateSeamFold;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.UseOnContext;

// The belt connector stores its first pulley in an item component, and that is the one coordinate the whole placement
// is built from. Everything after the read measures or walks the pair (stored, clicked): the 2x-maxLength gate that
// decides whether the anchor survives a second click, the length gate in canConnect, the subtraction canConnect derives
// the belt's axis, slope and diagonal test from, the step-by-step walk of the gap, and then createBelts doing all of it
// again to lay the blocks down. The two clicks are canonicalized independently, so across the seam the pair really is a
// world apart: the anchor is dropped, the gate refuses, and a belt cannot be built through the seam at all.
//
// Folded once, where the coordinate re-enters the world's frame. A component that crossed as an absolute position comes
// back at the nearest copy of itself to the block just clicked, and every site downstream is then doing ordinary
// arithmetic on a pair a few blocks apart. Nothing else in the method needs to know.
//
// The copy may lie past the bounds, and that is what makes the walk work: it steps from one side of the seam to the
// other without ever comparing equal to something a world away. Reads and writes at those coordinates land on the right
// block because Level wraps what it is handed — setBlock's position, getBlockState's chunk, and the block entity born
// inside the chunk's setBlockState, which LevelMixin relocates onto its physical key.
//
// Server-side by construction: useOn returns under world.isClientSide before this read is ever reached, so the level's
// own transformer is the truth here. The client's preview of the same component is folded in BeltConnectorHandlerMixin
// against the bounds the server sent.
//
// The component itself is never rewritten. It is stored canonical on the first click and compared with equals against
// real block positions, exactly as DirectionalShaftHalvesBlockEntity's source field is.
@Mixin(value = BeltConnectorItem.class, remap = false)
public class BeltConnectorItemMixin {
    @ModifyExpressionValue(
            method = "useOn",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;"))
    private Object toroidal$foldStoredPulley(Object stored, UseOnContext context) {
        if (!(stored instanceof BlockPos storedPulley)) {
            return stored;
        }

        return CreateSeamFold.foldPosition(context.getLevel(), context.getClickedPos(), storedPulley);
    }
}
