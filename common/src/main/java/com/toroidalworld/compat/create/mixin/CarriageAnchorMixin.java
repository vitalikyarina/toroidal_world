package com.toroidalworld.compat.create.mixin;

import java.lang.ref.WeakReference;

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.toroidalworld.compat.create.CarriageEntityFrame;
import com.toroidalworld.compat.create.CreateSeamFold;
import com.toroidalworld.core.DeckTransformation;
import com.toroidalworld.core.SeamTransform;
import com.toroidalworld.player.SeamSnap;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(targets = "com.simibubi.create.content.trains.entity.Carriage$DimensionalCarriageEntity", remap = false)
public abstract class CarriageAnchorMixin implements CarriageEntityFrame {
    @Shadow
    public Vec3 positionAnchor;

    @Shadow
    public TrackNodeLocation pivot;

    // Read for the level alone. The carriage entity's own class names a NeoForge interface the loader-free module cannot
    // see, so it is taken as the Entity it also is, through a reference whose type parameter is erased anyway.
    @Shadow
    public WeakReference<?> entity;

    // The two reads the chunk lookahead is built from, and the one that is written onto the entity. They are told apart
    // by ordinal because only the last of them moves the carriage, and moving it is what the riders have to follow.
    @ModifyExpressionValue(method = "alignEntity",
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
                    target = "Lcom/simibubi/create/content/trains/entity/Carriage$DimensionalCarriageEntity;positionAnchor:Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 0))
    private Vec3 toroidal$anchorForLookahead(Vec3 anchor) {
        return toroidal$anchorInLocalFrame(anchor);
    }

    @ModifyExpressionValue(method = "alignEntity",
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
                    target = "Lcom/simibubi/create/content/trains/entity/Carriage$DimensionalCarriageEntity;positionAnchor:Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 1))
    private Vec3 toroidal$anchorForLookaheadStep(Vec3 anchor) {
        return toroidal$anchorInLocalFrame(anchor);
    }

    @ModifyExpressionValue(method = "alignEntity",
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
                    target = "Lcom/simibubi/create/content/trains/entity/Carriage$DimensionalCarriageEntity;positionAnchor:Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 2))
    private Vec3 toroidal$anchorForWrite(Vec3 anchor) {
        Vec3 written = toroidal$anchorInLocalFrame(anchor);
        Entity carriageEntity = toroidal$entity();
        if (carriageEntity == null || carriageEntity.level().isClientSide()) {
            return written;
        }

        toroidal$carryAboard(carriageEntity, written);
        return written;
    }

    @ModifyExpressionValue(method = "alignEntity",
            at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/data/Couple;getSecond()Ljava/lang/Object;"))
    private Object toroidal$coupledAnchorInLeadingFrame(Object coupled, @Local(ordinal = 0) Vec3 leading) {
        Entity carriageEntity = toroidal$entity();
        if (carriageEntity == null || !(coupled instanceof Vec3 trailing)) {
            return coupled;
        }

        return CreateSeamFold.nearestCopy(carriageEntity.level(), leading, trailing);
    }

    @ModifyExpressionValue(method = "createEntity",
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
                    target = "Lcom/simibubi/create/content/trains/entity/Carriage$DimensionalCarriageEntity;positionAnchor:Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$anchorIntoWorldFrameOnCreate(Vec3 anchor, Level level, boolean loadPassengers) {
        return anchor != null && level instanceof ServerLevel serverLevel
                ? CreateSeamFold.canonical(serverLevel, anchor)
                : anchor;
    }

    @ModifyExpressionValue(method = "alignEntity",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/trains/entity/CarriageContraptionEntity;position()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$entityPositionInAnchorFrame(Vec3 position) {
        Entity carriageEntity = toroidal$entity();
        if (carriageEntity == null) {
            return position;
        }

        Vec3 anchor = this.positionAnchor;
        if (anchor == null) {
            return position;
        }

        Level level = carriageEntity.level();
        Vec3 worldFrameAnchor =
                level instanceof ServerLevel serverLevel ? CreateSeamFold.canonical(serverLevel, anchor) : anchor;
        return CreateSeamFold.nearestCopy(level, worldFrameAnchor, position);
    }

    @ModifyReturnValue(method = "leadingAnchor", at = @At("RETURN"))
    private Vec3 toroidal$leadingAnchorInClientFrame(Vec3 anchor) {
        return toroidal$anchorInClientFrame(anchor);
    }

    @ModifyReturnValue(method = "trailingAnchor", at = @At("RETURN"))
    private Vec3 toroidal$trailingAnchorInClientFrame(Vec3 anchor) {
        return toroidal$anchorInClientFrame(anchor);
    }

    @ModifyExpressionValue(method = "updateCutoff",
            at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/data/Couple;getSecond()Ljava/lang/Object;"))
    private Object toroidal$trailingAnchorInLeadingFrame(Object trailing, @Local(ordinal = 0) Vec3 leading) {
        if (!(trailing instanceof Vec3 trailingAnchor)) {
            return trailing;
        }

        return toroidal$inCutoffFrame(leading, trailingAnchor);
    }

    @ModifyExpressionValue(method = "updateCutoff",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;add(DDD)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$pivotInLeadingAnchorFrame(Vec3 pivotLoc, @Local(ordinal = 0) Vec3 leading) {
        return toroidal$inCutoffFrame(leading, pivotLoc);
    }

    @Override
    public @Nullable Level toroidal$carriageLevel() {
        Entity carriageEntity = toroidal$entity();
        return carriageEntity == null ? null : carriageEntity.level();
    }

    private Vec3 toroidal$inCutoffFrame(Vec3 anchor, Vec3 target) {
        return CreateSeamFold.nearestCopy(toroidal$carriageLevel(),
                this.pivot == null ? null : this.pivot.getDimension(), anchor, target);
    }

    private void toroidal$carryAboard(Entity carriage, Vec3 written) {
        Vec3 standing = carriage.position();
        Vec3 renamed = CreateSeamFold.nearestCopy(carriage.level(), written, standing);
        if (renamed.equals(standing)) {
            return;
        }

        Vec3 shift = renamed.subtract(standing);
        DeckTransformation lap = new DeckTransformation(
                SeamTransform.translation((int) Math.round(shift.x), (int) Math.round(shift.z)));
        for (Entity passenger : carriage.getPassengers()) {
            SeamSnap.withPassengers(passenger, lap);
        }

        if (carriage instanceof ContraptionColliderAccessor colliders) {
            for (Entity aboard : colliders.toroidal$collidingEntities().keySet()) {
                if (aboard.isPassenger() || aboard instanceof Player) {
                    continue;
                }

                SeamSnap.withPassengers(aboard, lap);
            }
        }
    }

    private @Nullable Vec3 toroidal$anchorInLocalFrame(@Nullable Vec3 anchor) {
        Entity carriageEntity = toroidal$entity();
        if (anchor == null || carriageEntity == null) {
            return anchor;
        }

        Level level = carriageEntity.level();
        return level instanceof ServerLevel serverLevel
                ? CreateSeamFold.canonical(serverLevel, anchor)
                : toroidal$anchorInClientFrame(anchor);
    }

    private @Nullable Vec3 toroidal$anchorInClientFrame(@Nullable Vec3 anchor) {
        Entity carriageEntity = toroidal$entity();
        if (anchor == null || carriageEntity == null || !carriageEntity.level().isClientSide()) {
            return anchor;
        }

        return CreateSeamFold.nearestCopy(carriageEntity.level(), carriageEntity.position(), anchor);
    }

    private @Nullable Entity toroidal$entity() {
        return this.entity.get() instanceof Entity carriageEntity ? carriageEntity : null;
    }
}
