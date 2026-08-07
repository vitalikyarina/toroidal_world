package com.toroidalworld.mixin;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.accessors.NavigationShifter;
import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

// A path is searched in a region around the mob. A target on the other side of the seam sits a whole world outside that
// region, so no path is ever found — or one is found the long way round. Each target becomes the copy nearest the mob,
// which may lie just past the bounds: that is exactly the direction the mob should walk.
@Mixin(PathNavigation.class)
public class PathNavigationMixin implements NavigationShifter {
    @Shadow
    @Final
    protected Mob mob;

    @Shadow
    protected @Nullable Path path;

    @Shadow
    private @Nullable BlockPos targetPos;

    @Shadow
    protected Vec3i timeoutCachedNode;

    @Shadow
    protected Vec3 lastStuckCheckPos;

    @ModifyVariable(
            method = "createPath(Ljava/util/Set;IZIF)Lnet/minecraft/world/level/pathfinder/Path;",
            at = @At("HEAD"), argsOnly = true)
    private Set<BlockPos> toroidal$targetsThroughSeam(Set<BlockPos> targets) {
        WorldLoopTransformer transformer = toroidal$wrappedTransformer();
        if (transformer == null || targets.isEmpty()) {
            return targets;
        }

        BlockPos from = this.mob.blockPosition();
        Set<BlockPos> unwrapped = null;
        int identityPrefix = 0;
        for (BlockPos target : targets) {
            BlockPos nearest = transformer.blocks.unwrap(from, target);
            if (unwrapped == null) {
                if (nearest == target) {
                    identityPrefix++;
                    continue;
                }

                unwrapped = new LinkedHashSet<>(targets.size());
                int copiedCount = 0;
                for (BlockPos passedTarget : targets) {
                    if (copiedCount++ == identityPrefix) {
                        break;
                    }

                    unwrapped.add(passedTarget);
                }
            }

            unwrapped.add(nearest);
        }

        return unwrapped == null ? targets : unwrapped;
    }

    // "Have I reached this node yet?" is a plain difference — and the moment the mob steps over the boundary and is
    // wrapped to the other side of the world, every node of its path is suddenly a world away. It never reaches the next
    // one, so the path never advances, while the move control keeps aiming it back across the seam: the mob walks over
    // the boundary, is wrapped, walks back, and spins on the spot forever. The distance is measured through the seam.
    @WrapOperation(
            method = "followThePath",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;abs(D)D", ordinal = 0))
    private double toroidal$nodeDistanceX(double delta, Operation<Double> original) {
        WorldLoopTransformer transformer = toroidal$wrappedTransformer();
        return transformer == null ? original.call(delta) : Math.abs(transformer.coords.x.foldDelta(delta));
    }

    @WrapOperation(
            method = "followThePath",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;abs(D)D", ordinal = 2))
    private double toroidal$nodeDistanceZ(double delta, Operation<Double> original) {
        WorldLoopTransformer transformer = toroidal$wrappedTransformer();
        return transformer == null ? original.call(delta) : Math.abs(transformer.coords.z.foldDelta(delta));
    }

    // A block changing anywhere near a mob asks its navigation whether the change is worth replanning for, and the
    // answer is a distance from the changed block to the middle of what is left of the path. The changed block is named
    // in the world; the path's far end may be unwrapped past the bounds, and the midpoint carries half of that offset —
    // so the two are read a world apart and the question is answered wrongly in both directions. A door opened in front
    // of a mob crossing the seam is ignored, and a block placed on the far side of the world triggers a replan.
    @WrapOperation(
            method = "shouldRecomputePath",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;closerToCenterThan(Lnet/minecraft/core/Position;D)Z"))
    private boolean toroidal$replanRangeThroughSeam(BlockPos changedPos, Position middlePos, double distance,
            Operation<Boolean> original) {
        return SeamRange.closerToCenterThan(this.mob, changedPos, middlePos, distance);
    }

    // The folds above keep the on-ground checks honest, but the follow chain also compares raw coordinates the fold
    // cannot reach: the mid-air drop advance (floor equality in tick), the overshoot recovery (Vec3 direction math),
    // and both safety nets (stuck / timeout anchors). Instead of chasing each read, the wrap funnel calls this to move
    // the whole navigation state by the same whole-world vector as the mob — one coordinate space again, every raw
    // comparison correct at once.
    @Override
    public void toroidal$shiftBy(int shiftX, int shiftZ) {
        if (this.targetPos != null) {
            this.targetPos = this.targetPos.offset(shiftX, 0, shiftZ);
        }

        this.lastStuckCheckPos = this.lastStuckCheckPos.add(shiftX, 0, shiftZ);
        if (!Vec3i.ZERO.equals(this.timeoutCachedNode)) {
            this.timeoutCachedNode = this.timeoutCachedNode.offset(shiftX, 0, shiftZ);
        }

        if (this.path != null && !this.path.isDone()) {
            this.path = toroidal$shifted(this.path, shiftX, shiftZ);
        }
    }

    // Node coordinates are final and Path caches only relative measures (distToTarget), so a shifted path is a rebuild:
    // cloned nodes, offset target, same progress index. debugData is dropped — it exists only while the path debug
    // renderer captures, and a wrap mid-capture is not worth preserving.
    @Unique
    private static Path toroidal$shifted(Path path, int shiftX, int shiftZ) {
        List<Node> nodes = new ArrayList<>(path.getNodeCount());
        for (int i = 0; i < path.getNodeCount(); i++) {
            Node node = path.getNode(i);
            nodes.add(node.cloneAndMove(node.x + shiftX, node.y, node.z + shiftZ));
        }

        Path shifted = new Path(nodes, path.getTarget().offset(shiftX, 0, shiftZ), path.canReach());
        shifted.setNextNodeIndex(path.getNextNodeIndex());
        return shifted;
    }

    @Unique
    private @Nullable WorldLoopTransformer toroidal$wrappedTransformer() {
        return ((TransformerSource) this.mob).toroidal$wrappedTransformer();
    }
}
