package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.entity.SeamAim;
import com.toroidalworld.player.VehicleDismountResync;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(Entity.class)
public class EntityMixin implements TransformerSource {
    @WrapMethod(method = "distanceTo")
    private float toroidal$distanceThroughSeam(Entity other, Operation<Float> original) {
        WorldLoopTransformer transformer = toroidal$wrappedTransformer();
        if (transformer == null) {
            return original.call(other);
        }

        return (float) Math.sqrt(toroidal$sqrTo(transformer, other.getX(), other.getY(), other.getZ()));
    }

    @WrapMethod(method = "distanceToSqr(DDD)D")
    private double toroidal$distanceToSqrThroughSeam(double x, double y, double z, Operation<Double> original) {
        WorldLoopTransformer transformer = toroidal$wrappedTransformer();
        if (transformer == null) {
            return original.call(x, y, z);
        }

        return toroidal$sqrTo(transformer, x, y, z);
    }

    @WrapMethod(method = "distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D")
    private double toroidal$distanceToSqrVecThroughSeam(Vec3 pos, Operation<Double> original) {
        WorldLoopTransformer transformer = toroidal$wrappedTransformer();
        if (transformer == null) {
            return original.call(pos);
        }

        return toroidal$sqrTo(transformer, pos.x, pos.y, pos.z);
    }

    @WrapMethod(method = "closerThan(Lnet/minecraft/world/entity/Entity;D)Z")
    private boolean toroidal$closerThanThroughSeam(Entity other, double distance, Operation<Boolean> original) {
        WorldLoopTransformer transformer = toroidal$wrappedTransformer();
        if (transformer == null) {
            return original.call(other, distance);
        }

        return toroidal$sqrTo(transformer, other.getX(), other.getY(), other.getZ()) < Mth.square(distance);
    }

    @WrapMethod(method = "closerThan(Lnet/minecraft/world/entity/Entity;DD)Z")
    private boolean toroidal$closerThanXZThroughSeam(Entity other, double distanceXZ, double distanceY,
            Operation<Boolean> original) {
        WorldLoopTransformer transformer = toroidal$wrappedTransformer();
        if (transformer == null) {
            return original.call(other, distanceXZ, distanceY);
        }

        Entity self = (Entity) (Object) this;
        double deltaX = transformer.coords.x.deltaFromBounds(self.getX(), other.getX());
        double deltaZ = transformer.coords.z.deltaFromBounds(self.getZ(), other.getZ());
        double deltaY = other.getY() - self.getY();
        return Mth.lengthSquared(deltaX, deltaZ) < Mth.square(distanceXZ) && Mth.square(deltaY) < Mth.square(distanceY);
    }

    @ModifyVariable(method = "lookAt(Lnet/minecraft/commands/arguments/EntityAnchorArgument$Anchor;Lnet/minecraft/world/phys/Vec3;)V",
            at = @At("HEAD"), argsOnly = true)
    private Vec3 toroidal$lookAtNearestCopy(Vec3 pos) {
        return SeamAim.nearestTo((Entity) (Object) this, pos);
    }

    @ModifyVariable(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("STORE"), ordinal = 0)
    private double toroidal$pushDeltaX(double deltaX) {
        WorldLoopTransformer transformer = toroidal$wrappedTransformer();
        return transformer == null ? deltaX : transformer.coords.x.foldDelta(deltaX);
    }

    @ModifyVariable(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("STORE"), ordinal = 1)
    private double toroidal$pushDeltaZ(double deltaZ) {
        WorldLoopTransformer transformer = toroidal$wrappedTransformer();
        return transformer == null ? deltaZ : transformer.coords.z.foldDelta(deltaZ);
    }

    @Inject(method = "removeVehicle", at = @At("HEAD"))
    private void toroidal$resyncVehicleOnDismount(CallbackInfo ci) {
        VehicleDismountResync.resyncAfterDismount((Entity) (Object) this);
    }

    @Unique
    private @Nullable Level toroidal$transformerLevel;

    @Unique
    private WorldLoopTransformer toroidal$transformer;

    @Override
    public @Nullable WorldLoopTransformer toroidal$wrappedTransformer() {
        Level level = ((Entity) (Object) this).level();
        if (level != this.toroidal$transformerLevel) {
            this.toroidal$transformer = WorldLoopAttachments.transformerOf(level);
            this.toroidal$transformerLevel = level;
        }

        WorldLoopTransformer transformer = this.toroidal$transformer;
        return transformer.isWrapped() ? transformer : null;
    }

    @Unique
    private double toroidal$sqrTo(WorldLoopTransformer transformer, double x, double y, double z) {
        Entity self = (Entity) (Object) this;
        return transformer.coords.sqrDistToBounds(self.getX(), self.getY(), self.getZ(), x, y, z);
    }
}
