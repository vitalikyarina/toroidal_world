package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldFold;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

@Mixin(Player.class)
public class PlayerMixin {
    @WrapMethod(method = "isWithinBlockInteractionRange")
    private boolean toroidal$wrappedBlockReach(BlockPos pos, double buffer, Operation<Boolean> original) {
        Player player = (Player) (Object) this;
        WorldFold transformer = ((TransformerSource) player).toroidal$wrappedTransformer();
        if (transformer == null) {
            return original.call(pos, buffer);
        }

        double maxRange = player.blockInteractionRange() + buffer;
        return transformer.sqrDistanceToBox(new AABB(pos), player.getEyePosition()) < maxRange * maxRange;
    }

    @WrapMethod(method = "isWithinEntityInteractionRange(Lnet/minecraft/world/phys/AABB;D)Z")
    private boolean toroidal$wrappedEntityReach(AABB aabb, double buffer, Operation<Boolean> original) {
        Player player = (Player) (Object) this;
        WorldFold transformer = ((TransformerSource) player).toroidal$wrappedTransformer();
        return transformer == null
                ? original.call(aabb, buffer)
                : original.call(transformer.foldBox(player.getEyePosition(), aabb).value(), buffer);
    }

    @WrapMethod(method = "isWithinAttackRange(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/phys/AABB;D)Z")
    private boolean toroidal$wrappedAttackReach(ItemStack weapon, AABB aabb, double buffer, Operation<Boolean> original) {
        Player player = (Player) (Object) this;
        WorldFold transformer = ((TransformerSource) player).toroidal$wrappedTransformer();
        return transformer == null
                ? original.call(weapon, aabb, buffer)
                : original.call(weapon, transformer.foldBox(player.getEyePosition(), aabb).value(), buffer);
    }
}
