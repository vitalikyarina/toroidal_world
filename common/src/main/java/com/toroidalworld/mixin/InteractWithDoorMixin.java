package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.entity.SeamRange;
import com.toroidalworld.entity.SeamSteering;
import com.toroidalworld.probe.ReseatProbe;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.InteractWithDoor;

// A villager remembers every door it opens and closes it once it is three blocks past. "Too far" is a raw difference
// against the door's own position, so across the seam every remembered door reads a world away — and a door judged too
// far is dropped from the memory without being closed. Doors near the boundary are opened and left standing open, which
// is the one thing this behaviour exists to prevent.
//
// The second reading is the courtesy that holds a door for whoever is coming through it. It measures the door against
// another mob's position rather than the villager's own, so the level source is that mob; read raw it never sees anyone
// and the door is shut in the face of the villager behind.
@Mixin(InteractWithDoor.class)
public class InteractWithDoorMixin {
    @WrapOperation(
            method = "*",
            require = 2,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;closerToCenterThan(Lnet/minecraft/core/Position;D)Z"),
            expect = 2)
    private static boolean toroidal$doorReachThroughSeam(BlockPos doorPos, Position bodyPosition, double distance,
            Operation<Boolean> original, @Local(argsOnly = true) LivingEntity body) {
        return ReseatProbe.decided(body.level(), ReseatProbe.DOOR_REACH,
                original.call(doorPos, bodyPosition, distance),
                SeamRange.closerToCenterThan(body, doorPos, bodyPosition, distance));
    }

    // A door is not closed while the mob is standing in it, and standing in it is asked as an equality between the
    // remembered door and the node the mob is walking off or onto. Both sides come from path nodes — the memory records
    // whichever coordinate the node carried when the door was opened — so they agree until the mob crosses the boundary
    // and the wrap funnel shifts the path without touching the memory. From that tick the two names of one doorway
    // differ by the width of the world, the guard stops holding, and the mob shuts the door on itself as it steps
    // through: exactly the door at the seam, exactly the mob that just used it.
    //
    // Equality cannot be folded, so the node is restated as its copy nearest the door being examined. Where the two
    // already share a frame it is the same object back, and the comparison reads as it always did.
    @ModifyExpressionValue(
            method = "closeDoorsThatIHaveOpenedOrPassedThrough",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/pathfinder/Node;asBlockPos()Lnet/minecraft/core/BlockPos;"))
    private static BlockPos toroidal$standingInDoorwayThroughSeam(BlockPos nodePos,
            @Local(argsOnly = true) LivingEntity body, @Local BlockPos doorPos) {
        WorldLoopTransformer transformer = ((TransformerSource) body).toroidal$wrappedTransformer();
        return transformer == null ? nodePos : transformer.blocks.nearestCopy(doorPos, nodePos);
    }

    // The courtesy is decided in two steps and the range above is only the first: whoever passed it is then asked
    // whether the door is the node it is walking off or onto, and that is the same equality against path nodes — this
    // time the other mob's, which the wrap funnel shifted when that mob crossed the boundary. The door is named where it
    // stands, so the two never match and the holder-open is refused for exactly the mob it was measured close enough to.
    //
    // The whole of what the call does with the door is compare it to those two nodes, so it is handed the door in the
    // frame the nodes are in — the copy nearest the mob whose path is being read.
    @WrapOperation(
            method = "*",
            require = 1,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/behavior/InteractWithDoor;"
                            + "isMobComingThroughDoor(Lnet/minecraft/world/entity/ai/Brain;Lnet/minecraft/core/BlockPos;)Z"))
    private static boolean toroidal$otherMobsDoorwayThroughSeam(Brain<?> otherBrain, BlockPos doorPos,
            Operation<Boolean> original, @Local(argsOnly = true) LivingEntity otherMob) {
        BlockPos nearest = ReseatProbe.decided(otherMob.level(), ReseatProbe.DOOR_OTHER_MOB, doorPos,
                SeamSteering.nearestCopy(otherMob, doorPos));
        return original.call(otherBrain, nearest);
    }
}
