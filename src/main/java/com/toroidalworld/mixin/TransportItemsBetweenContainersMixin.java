package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.entity.SeamAim;
import com.toroidalworld.entity.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

// The chunks around the carrier are scanned for chests, and the scan itself reaches across the seam — a chunk past the
// bounds is wrapped on its way to the chunk source. What comes back is a block entity sitting at its real coordinates
// inside the world, and every question this behaviour then asks about those coordinates is a raw one. There are four,
// and a chest across the seam has to survive all of them.
//
// Finding it: the search box drawn around the carrier reaches past the bounds, so a chest on the far side falls outside
// it, and the nearest-chest comparison reads it a world away. Both are answered by the same missing fact — where that
// chest is, seen from here — so the fold is taken on the position as it is read, once per reader.
//
// Keeping it: the target must then be judged reachable, and that judgement is geometry, not distance. The reach test
// intersects a box grown around the chest with one at the end of the path — and the path was built to the copy nearest
// the carrier, so the two boxes are a world apart and never meet. The sight test then draws rays at the chest's centre
// and demands the block they strike be the chest itself; across the seam the ray spans half the map, and even folded it
// strikes a position outside the bounds that no longer equals what the target recorded. A target failing either is
// written into the unreachable memory, which is why the carrier does not merely hesitate — it blacklists the chest,
// finds nothing else, waits out its cooldown and starts the same loop again.
//
// Only local copies move. The position the target records for itself is read again from the block entity, so what gets
// written into the visited and unreachable memories stays inside the world where the next tick will find it.
@Mixin(TransportItemsBetweenContainers.class)
public class TransportItemsBetweenContainersMixin {
    @ModifyExpressionValue(
            method = "getTransportTarget",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/ChestBlockEntity;getBlockPos()Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$candidateChestThroughSeam(BlockPos chestPos, @Local(argsOnly = true) PathfinderMob body) {
        return SeamSteering.nearestCopy(body, chestPos);
    }

    @ModifyExpressionValue(
            method = "isTargetValidToPick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/BlockEntity;getBlockPos()Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$searchAreaPosThroughSeam(BlockPos chestPos, @Local(argsOnly = true) PathfinderMob body) {
        return SeamSteering.nearestCopy(body, chestPos);
    }

    // The reach test, asked of the carrier's own position while it decides whether to walk and of the path's end while
    // it decides whether the walk can arrive. The box grown around the chest is laid down beside whichever point is
    // asking, and vanilla's intersection runs on that — the same fold the melee reach takes.
    @ModifyExpressionValue(
            method = "isWithinTargetDistance",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;move(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/AABB;"))
    private AABB toroidal$reachBoxThroughSeam(AABB targetBox, @Local(argsOnly = true) PathfinderMob body,
            @Local(argsOnly = true) Vec3 fromPos) {
        WorldLoopTransformer transformer = ((TransformerSource) body).toroidal$wrappedTransformer();
        if (transformer == null) {
            return targetBox;
        }

        return transformer.foldBoxToward(fromPos, targetBox);
    }

    // The sight test draws a ray at each face of the chest. Its centre becomes the copy nearest the carrier, so the ray
    // is the short one through the seam rather than one spanning half the map.
    @ModifyExpressionValue(
            method = "canSeeAnyTargetSide",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;atCenterOf(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$sightCentreThroughSeam(Vec3 centre, @Local(argsOnly = true) PathfinderMob body) {
        return SeamAim.nearestTo(body, centre);
    }

    // With the ray folded it strikes the right ground, but it names it by a coordinate past the bounds — and the caller
    // compares that name against the one the target wrote down, which is inside them. Block reads along a ray wrap on
    // their way to a chunk, so this only restates the hit as the block that physically exists.
    @ModifyExpressionValue(
            method = "lambda$canSeeAnyTargetSide$1",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;clip(Lnet/minecraft/world/level/ClipContext;)Lnet/minecraft/world/phys/BlockHitResult;"))
    private static BlockHitResult toroidal$sightHitThroughSeam(BlockHitResult hit,
            @Local(argsOnly = true) PathfinderMob body) {
        WorldLoopTransformer transformer = ((TransformerSource) body).toroidal$wrappedTransformer();
        if (transformer == null) {
            return hit;
        }

        BlockPos hitPos = hit.getBlockPos();
        BlockPos wrapped = transformer.blocks.wrap(hitPos);
        return wrapped == hitPos ? hit : hit.withPosition(wrapped);
    }
}
