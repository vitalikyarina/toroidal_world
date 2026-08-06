package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldLoopTransformer;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

// Standing next to the seam, the block in front of you is a whole world away by plain distance, so vanilla would call
// it out of reach. The distance has to be measured through the seam instead — for the block, and for a mob you attack,
// feed or trade with (all of which the server gates on the same eye-to-target-box range check).
@Mixin(Player.class)
public class PlayerMixin {
    @WrapMethod(method = "isWithinBlockInteractionRange")
    private boolean toroidal$wrappedBlockReach(BlockPos pos, double buffer, Operation<Boolean> original) {
        Player player = (Player) (Object) this;
        WorldLoopTransformer transformer = ((TransformerSource) player).toroidal$wrappedTransformer();
        if (transformer == null) {
            return original.call(pos, buffer);
        }

        double maxRange = player.blockInteractionRange() + buffer;
        return transformer.distanceToSqrWrappedCoord(new AABB(pos), player.getEyePosition()) < maxRange * maxRange;
    }

    // The target's real box sits a world away across the seam, so the range check drops the interaction — no attack, no
    // feeding, no trade. Fold the box to the copy nearest the eye and let the vanilla check run on it: a cross-seam mob
    // reads as the step away it really is, and a same-side target is unchanged, so reach elsewhere is exactly vanilla.
    @WrapMethod(method = "isWithinEntityInteractionRange(Lnet/minecraft/world/phys/AABB;D)Z")
    private boolean toroidal$wrappedEntityReach(AABB aabb, double buffer, Operation<Boolean> original) {
        Player player = (Player) (Object) this;
        WorldLoopTransformer transformer = ((TransformerSource) player).toroidal$wrappedTransformer();
        return transformer == null
                ? original.call(aabb, buffer)
                : original.call(transformer.foldBoxToward(player.getEyePosition(), aabb), buffer);
    }

    @WrapMethod(method = "isWithinAttackRange(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/phys/AABB;D)Z")
    private boolean toroidal$wrappedAttackReach(ItemStack weapon, AABB aabb, double buffer, Operation<Boolean> original) {
        Player player = (Player) (Object) this;
        WorldLoopTransformer transformer = ((TransformerSource) player).toroidal$wrappedTransformer();
        return transformer == null
                ? original.call(weapon, aabb, buffer)
                : original.call(weapon, transformer.foldBoxToward(player.getEyePosition(), aabb), buffer);
    }
}
