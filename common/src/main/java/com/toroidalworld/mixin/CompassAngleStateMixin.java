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

// A compass target is an absolute coordinate the client was handed and then kept — the lodestone tracker inside the
// item, the last death location, the world spawn. The client's own coordinate is unbounded and gains a whole world
// width per lap, so a stored target names a copy of itself that may be any number of laps from where the owner now
// stands; nothing refreshes it as the player walks.
//
// Two separate pieces of vanilla arithmetic then read that coordinate raw: the angle the needle points, and the
// "am I standing on it" test that makes the needle spin instead. Folding the angle alone leaves the second one wrong,
// and folding a delta only ever subtracts one width — true while the target is under one and a half worlds out, a lie
// past it.
//
// So the fold goes where the target is produced rather than where it is used: this is the one call every target type
// funnels through (spawn, lodestone, recovery, none, in hand or in an item frame), and the copy nearest the owner is
// what both readers below are then handed. nearestCopy wraps before it unwraps, so any number of laps folds in one
// step, and a target already nearest comes back as the very same position — vanilla's own arithmetic runs untouched.
//
// A target in another dimension is left to the check downstream, which rejects it before either reading. The level
// itself stays unwrapped on the client: only the bounds mirror seeded by WrappingSettingsPayload is read.
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

        // The owner's level rather than the render's: the dimension check downstream is asked of that one, and a
        // target folded against bounds the check does not use would be folded against the wrong world.
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedClientBoundsTransformerOf(owner.level());
        if (transformer == null) {
            return target;
        }

        BlockPos stored = target.pos();
        BlockPos nearest = transformer.blocks.nearestCopy(BlockPos.containing(owner.position()), stored);
        return nearest == stored ? target : GlobalPos.of(target.dimension(), nearest);
    }
}
