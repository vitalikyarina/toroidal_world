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
import com.toroidalworld.compat.create.CreateTrackFold;
import com.toroidalworld.player.SeamSnap;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// Where a carriage stops being graph state and becomes a place in the world. The graph answers in its own continuous
// frame — an edge that crosses the seam is anchored on one of its nodes and runs past the bounds toward the other, which
// is what keeps the arithmetic along it ordinary — so the position it hands out for a carriage on that stretch names
// ground outside the world. Create then writes it straight onto the entity every tick, and because its railway tick
// hangs off LevelTickEvent.Post, it writes after the wrap at the tail of tickNonPassenger: the wrap moves the carriage
// back inside, Create puts it outside again, and each of those is a whole-world shift the tracker sends as a teleport.
//
// So the anchor is wrapped at the moment it is handed over, and only there. Everything the carriage compares against
// itself — the two rotation anchors, the leading and trailing anchor, the bogey spacing — stays in the graph's frame and
// keeps reading the short way; the entity, whose chunk, tracker and save are all keyed by ground the world has, gets the
// wrapped copy. A crossing then costs the one teleport any entity costs, and the wrap after it has nothing to undo.
//
// The entity's own position is folded to the anchor's frame for the same reason the two must be comparable: on the tick
// the wrap lands, the entity still holds last tick's copy a world away, and the lookahead that decides whether to wait
// for chunks would point backwards across the world.
@Mixin(targets = "com.simibubi.create.content.trains.entity.Carriage$DimensionalCarriageEntity", remap = false)
public abstract class CarriageAnchorMixin {
    @Shadow
    public Vec3 positionAnchor;

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

    // The write itself, and the one moment of a crossing where anything aboard has to be carried along.
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

    // The carriage's own heading, taken as the difference between its two rotation anchors. They are the two travelling
    // points of one bogey, and while it sits over the seam node they are answered from two edges whose frames are a
    // world apart: the difference reads 512 blocks where the carriage is two blocks long, and atan2 of it turns the
    // contraption through 180 degrees. The collision boxes turn with it, so the deck swings out from under whoever is
    // standing on the train — which on the client, the side that decides where a player stands, is the driver.
    //
    // The pair is folded here rather than where it is stored, for the same reason as every other pair in this compat:
    // the stored anchors are canonical names of two places, and only their combination is one quantity.
    @ModifyExpressionValue(method = "alignEntity",
            at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/data/Couple;getSecond()Ljava/lang/Object;"))
    private Object toroidal$coupledAnchorInLeadingFrame(Object coupled, @Local(ordinal = 0) Vec3 leading) {
        Entity carriageEntity = toroidal$entity();
        if (carriageEntity == null || !(coupled instanceof Vec3 trailing)) {
            return coupled;
        }

        return CreateTrackFold.nearestCopy(carriageEntity.level(), leading, trailing);
    }

    // The entity does not exist yet here, so the frame comes from the method's own level rather than from it.
    @ModifyExpressionValue(method = "createEntity",
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
                    target = "Lcom/simibubi/create/content/trains/entity/Carriage$DimensionalCarriageEntity;positionAnchor:Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$anchorIntoWorldFrameOnCreate(Vec3 anchor, Level level, boolean loadPassengers) {
        return anchor == null || level.isClientSide() ? anchor : CreateTrackFold.wrap(level, anchor);
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
        return CreateTrackFold.nearestCopy(level, CreateTrackFold.wrap(level, anchor), position);
    }

    // The pair through which everything the client says about where the carriage is speaks: its sound is played at these
    // two points and faded by their distance to the camera, and its smoke rises from the leading one. They are the
    // stored anchors, in the graph's canonical frame — and a client that has walked past the boundary is a whole world
    // from canonical, because it is told the world is infinite and renders straight through the seam. The sound then
    // plays 512 blocks from the player driving the train, which is to say it is not heard at all.
    //
    // Only the client is answered in its own frame. On the server these two are read against each other rather than
    // against a place — the carriage's own stretch, measured by getAnchorDiff — and that pair is folded where it is
    // combined.
    @ModifyReturnValue(method = "leadingAnchor", at = @At("RETURN"))
    private Vec3 toroidal$leadingAnchorInClientFrame(Vec3 anchor) {
        return toroidal$anchorInClientFrame(anchor);
    }

    @ModifyReturnValue(method = "trailingAnchor", at = @At("RETURN"))
    private Vec3 toroidal$trailingAnchorInClientFrame(Vec3 anchor) {
        return toroidal$anchorInClientFrame(anchor);
    }

    // What travels when the carriage is renamed, and what does not.
    //
    // The vector is read from the entity's own position, never from the anchor's correction: the anchor stands outside
    // the world for every tick of a crossing, while the entity is renamed on exactly one of them and sits inside the
    // world on the rest. Taking the anchor's correction moved everything aboard once per out-of-bounds tick, three and
    // four times over, and left the driver hundreds of blocks past the far side. nearestCopy hands back the argument
    // itself when nothing folds, so this stays silent on every other tick and cannot apply twice.
    //
    // It reaches those aboard whose position the server owns: the passengers, and whatever rests on the deck without
    // riding it — a mob, a dropped item — moved the mod's own way across the seam, absMoveTo, so nothing interpolates
    // across the world and a passenger's chunk source follows.
    //
    // A player standing at the controls is deliberately not among them. Create's collider skips every player it meets on
    // the server and leaves that half of the physics to the client, so the server does not own where such a player
    // stands and must not move them: they cross by their own path, the one every walking player takes, where the wrap
    // also realigns the movement bounds the next packet is measured against.
    private void toroidal$carryAboard(Entity carriage, Vec3 written) {
        Vec3 standing = carriage.position();
        Vec3 renamed = CreateTrackFold.nearestCopy(carriage.level(), written, standing);
        if (renamed == standing) {
            return;
        }

        Vec3 shift = renamed.subtract(standing);
        for (Entity passenger : carriage.getPassengers()) {
            SeamSnap.withPassengers(passenger, shift);
        }

        if (carriage instanceof ContraptionColliderAccessor colliders) {
            for (Entity aboard : colliders.toroidal$collidingEntities().keySet()) {
                if (aboard.isPassenger() || aboard instanceof Player) {
                    continue;
                }

                SeamSnap.withPassengers(aboard, shift);
            }
        }
    }

    // Both sides read the carriage's place out of the same graph, and neither may take the answer as it comes.
    //
    // On the server the graph's frame runs a little past the bounds by design, and the entity may not, so the anchor is
    // wrapped into the world.
    //
    // On the client the world has no bounds at all — it is told the world is infinite and renders straight through the
    // seam — while the graph it holds is the server's, in canonical coordinates. A player who has walked past the
    // boundary is therefore a world away from the very carriage they are riding: Create puts it at the canonical copy,
    // the translated position packets put it at the near one, and the two rewrite each other every tick. So on that side
    // the anchor is moved to the copy nearest where the client already holds the carriage, which is the frame every
    // other thing it sees is in.
    private @Nullable Vec3 toroidal$anchorInLocalFrame(@Nullable Vec3 anchor) {
        Entity carriageEntity = toroidal$entity();
        if (anchor == null || carriageEntity == null) {
            return anchor;
        }

        Level level = carriageEntity.level();
        return level.isClientSide() ? toroidal$anchorInClientFrame(anchor) : CreateTrackFold.wrap(level, anchor);
    }

    private @Nullable Vec3 toroidal$anchorInClientFrame(@Nullable Vec3 anchor) {
        Entity carriageEntity = toroidal$entity();
        if (anchor == null || carriageEntity == null || !carriageEntity.level().isClientSide()) {
            return anchor;
        }

        return CreateTrackFold.nearestCopy(carriageEntity.level(), carriageEntity.position(), anchor);
    }

    private @Nullable Entity toroidal$entity() {
        return this.entity.get() instanceof Entity carriageEntity ? carriageEntity : null;
    }
}
