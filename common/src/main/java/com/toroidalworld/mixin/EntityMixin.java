package com.toroidalworld.mixin;

import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.entity.SeamAim;
import com.toroidalworld.player.VehicleDismountResync;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Mixin(Entity.class)
public class EntityMixin implements TransformerSource {
    @WrapMethod(method = "distanceTo")
    private float toroidal$distanceThroughSeam(Entity other, Operation<Float> original) {
        WorldFold transformer = toroidal$wrappedTransformer();
        if (transformer == null) {
            return original.call(other);
        }

        Vec3 otherPos = other.position();
        Vec3 nearest = toroidal$nearestCopy(transformer, otherPos);
        if (nearest == otherPos) {
            return original.call(other);
        }

        return (float) Math.sqrt(((Entity) (Object) this).distanceToSqr(nearest));
    }

    @WrapMethod(method = "distanceToSqr(DDD)D")
    private double toroidal$distanceToSqrThroughSeam(double x, double y, double z, Operation<Double> original) {
        WorldFold transformer = toroidal$wrappedTransformer();
        if (transformer == null) {
            return original.call(x, y, z);
        }

        Vec3 nearest = toroidal$nearestCopy(transformer, new Vec3(x, y, z));
        return original.call(nearest.x, nearest.y, nearest.z);
    }

    @WrapMethod(method = "distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D")
    private double toroidal$distanceToSqrVecThroughSeam(Vec3 pos, Operation<Double> original) {
        WorldFold transformer = toroidal$wrappedTransformer();
        if (transformer == null) {
            return original.call(pos);
        }

        return original.call(toroidal$nearestCopy(transformer, pos));
    }

    @WrapMethod(method = "closerThan(Lnet/minecraft/world/entity/Entity;D)Z")
    private boolean toroidal$closerThanThroughSeam(Entity other, double distance, Operation<Boolean> original) {
        WorldFold transformer = toroidal$wrappedTransformer();
        if (transformer == null) {
            return original.call(other, distance);
        }

        Vec3 otherPos = other.position();
        Vec3 nearest = toroidal$nearestCopy(transformer, otherPos);
        if (nearest == otherPos) {
            return original.call(other, distance);
        }

        return ((Entity) (Object) this).position().closerThan(nearest, distance);
    }

    @WrapMethod(method = "closerThan(Lnet/minecraft/world/entity/Entity;DD)Z")
    private boolean toroidal$closerThanXZThroughSeam(Entity other, double distanceXZ, double distanceY,
            Operation<Boolean> original) {
        WorldFold transformer = toroidal$wrappedTransformer();
        if (transformer == null) {
            return original.call(other, distanceXZ, distanceY);
        }

        Vec3 otherPos = other.position();
        Vec3 nearest = toroidal$nearestCopy(transformer, otherPos);
        if (nearest == otherPos) {
            return original.call(other, distanceXZ, distanceY);
        }

        Vec3 delta = nearest.subtract(((Entity) (Object) this).position());
        return Mth.lengthSquared(delta.x, delta.z) < Mth.square(distanceXZ)
                && Mth.square(delta.y) < Mth.square(distanceY);
    }

    @ModifyVariable(method = "lookAt(Lnet/minecraft/commands/arguments/EntityAnchorArgument$Anchor;Lnet/minecraft/world/phys/Vec3;)V",
            at = @At("HEAD"), argsOnly = true)
    private Vec3 toroidal$lookAtNearestCopy(Vec3 pos) {
        return SeamAim.nearestTo((Entity) (Object) this, pos);
    }

    @ModifyVariable(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("STORE"), ordinal = 0)
    private double toroidal$pushDeltaX(double deltaX, @Local(argsOnly = true) Entity other) {
        WorldFold transformer = toroidal$wrappedTransformer();
        return transformer == null ? deltaX : toroidal$deltaTo(transformer, other).x;
    }

    @ModifyVariable(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("STORE"), ordinal = 1)
    private double toroidal$pushDeltaZ(double deltaZ, @Local(argsOnly = true) Entity other) {
        WorldFold transformer = toroidal$wrappedTransformer();
        return transformer == null ? deltaZ : toroidal$deltaTo(transformer, other).z;
    }

    @WrapOperation(
            method = "checkSupportingBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;findSupportingBlock(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/Optional;"))
    private Optional<BlockPos> toroidal$storeCanonicalSupportingBlock(Level level, Entity source, AABB box,
            Operation<Optional<BlockPos>> original) {
        Optional<BlockPos> found = original.call(level, source, box);
        WorldFold transformer = toroidal$wrappedTransformer();
        if (transformer == null || found.isEmpty()) {
            return found;
        }

        BlockPos raw = found.get();
        BlockPos folded = transformer.fold(raw);
        return folded == raw ? found : Optional.of(folded);
    }

    @ModifyReturnValue(method = "getOnPos(F)Lnet/minecraft/core/BlockPos;", at = @At("RETURN"))
    private BlockPos toroidal$canonicalOnPos(BlockPos raw) {
        WorldFold transformer = toroidal$wrappedTransformer();
        return transformer == null ? raw : transformer.fold(raw);
    }

    @Inject(method = "removeVehicle", at = @At("HEAD"))
    private void toroidal$resyncVehicleOnDismount(CallbackInfo ci) {
        VehicleDismountResync.resyncAfterDismount((Entity) (Object) this);
    }

    @Unique
    private @Nullable Level toroidal$transformerLevel;

    @Unique
    private WorldFold toroidal$transformer;

    @Override
    public @Nullable WorldFold toroidal$wrappedTransformer() {
        Level level = ((Entity) (Object) this).level();
        if (level != this.toroidal$transformerLevel) {
            this.toroidal$transformer = WorldLoopAttachments.transformerOf(level);
            this.toroidal$transformerLevel = level;
        }

        WorldFold transformer = this.toroidal$transformer;
        return transformer.isWrapped() ? transformer : null;
    }

    @Unique
    private Vec3 toroidal$deltaTo(WorldFold transformer, Entity other) {
        return transformer.foldDelta(((Entity) (Object) this).position(), other.position());
    }

    @Unique
    private Vec3 toroidal$nearestCopy(WorldFold transformer, Vec3 target) {
        return transformer.nearestCopy(((Entity) (Object) this).position(), target);
    }
}
