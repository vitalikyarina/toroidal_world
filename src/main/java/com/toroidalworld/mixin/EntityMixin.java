package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.entity.SeamAim;
import com.toroidalworld.player.VehicleDismountResync;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// Two things an entity does that the seam changes.
//
// The first is distance. In a looped world there is no other kind: the way between two points is the shortest one, and
// it may run through the seam. Every question the game asks about how far something is — a mob deciding whether to
// despawn, a chicken following seeds, anything hunting a target — comes down to these five methods, each carrying its
// own copy of the same arithmetic. Fixing the notion itself is what keeps us from chasing the same bug through the
// whole of vanilla's AI.
//
// The second is being placed somewhere unrelated to where you were, which for a player means the client's mirror has to
// start again — see toroidal$rebaseMirrorOnPlacement at the bottom.
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

    // The horizontal reach wraps; the vertical one has nowhere to wrap to.
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

    // Turning to face a point is yet another copy of the same arithmetic, this one on the entity itself — the same six
    // lines CommandSourceStack.facing carries for a source with no entity behind it, and folded the same way: the point
    // becomes its copy nearest the turning body before vanilla subtracts, so the yaw, the pitch and the horizontal leg
    // all fall out of vanilla's own arithmetic already naming one world copy.
    //
    // The nearest copy rather than a fold of the two deltas, which only ever subtracts a single width: /tp lands the
    // body before the tick tail wraps it, so `/tp @s ~300 ~ ~ facing entity <e>` turns from a position laps outside the
    // world and one width off it is still one width wrong. nearestCopy wraps both ends first.
    //
    // Entity.lookAt is the choke point: ServerPlayer overrides it twice and LivingEntity once, and every one of them
    // reaches this method by super. The packet those overrides then send the client is moved separately, in
    // PacketTranslator.playerLookAt.
    @ModifyVariable(method = "lookAt(Lnet/minecraft/commands/arguments/EntityAnchorArgument$Anchor;Lnet/minecraft/world/phys/Vec3;)V",
            at = @At("HEAD"), argsOnly = true)
    private Vec3 toroidal$lookAtNearestCopy(Vec3 pos) {
        return SeamAim.nearestTo((Entity) (Object) this, pos);
    }

    // Shoving another entity is the same delta once more. Across the seam the raw gap is a whole world, so the shove
    // points the long way round and the 1/distance falloff scales it to nothing — two mobs a step apart on the torus
    // drift through each other. Folding both components aims the push the short way with its true strength. The candidate
    // query that even finds a cross-seam neighbour is already seam-aware (LevelMixin); only this direction was left raw.
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

    // The seam wrap relocates an entity without recording movement, so the bridge segment applyEffectsFromBlocks
    // synthesizes from the last recorded position to the current one spans the whole world — and the block sweep walks
    // its first 16 blocks, handing any portal within reach of the far boundary a phantom trigger: the spurious portal
    // teleport while merely crossing the seam. Folding the bridge start around the current position makes the segment
    // cover exactly the ground actually traversed through the seam; an ordinary bridge folds to itself.
    @ModifyArg(
            method = "applyEffectsFromBlocks()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity$Movement;<init>(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)V"),
            index = 0)
    private Vec3 toroidal$foldBridgeThroughSeam(Vec3 from) {
        WorldLoopTransformer transformer = toroidal$wrappedTransformer();
        if (transformer == null) {
            return from;
        }

        Vec3 position = ((Entity) (Object) this).position();
        return transformer.vectors.nearestCopy(position, from);
    }

    // The mirror the client's coordinates are unwrapped around has to be right *before the first packet describing the
    // new state*, not merely before the position packet — on respawn the chunk-cache centre goes out first, and a mirror
    // still holding the pre-death coordinate centres the client a whole world from the chunks it is then sent.
    //
    // snapTo is where the server places a player somewhere unrelated to where they were: respawn, and the initial
    // placement. Ordinary mid-play corrections do not come through here — they go through teleportSetPosition, and
    // rebasing on those would be the very "fling the client a world back" this whole layer exists to avoid.
    @Inject(method = "snapTo(DDDFF)V", at = @At("TAIL"))
    private void toroidal$rebaseMirrorOnPlacement(double x, double y, double z, float yRot, float xRot, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player) {
            WorldLoopAttachments.rebaseClientPositionOf(player);
        }
    }

    // At the head, before the detach nulls the vehicle field: the resync has to read which vehicle is being left, and
    // its send must be deferred past the detach — both handled inside the helper. Vanilla fires no callback here, and
    // the passenger is the one party that knows the moment.
    @Inject(method = "removeVehicle", at = @At("HEAD"))
    private void toroidal$resyncVehicleOnDismount(CallbackInfo ci) {
        VehicleDismountResync.resyncAfterDismount((Entity) (Object) this);
    }

    @Unique
    private @Nullable Level toroidal$transformerLevel;

    @Unique
    private WorldLoopTransformer toroidal$transformer;

    // The distance methods run per AI tick for every entity, but an entity can change levels (a portal), so the cache is
    // a (level, transformer) pair re-resolved when level() changes. Deliberately not volatile: resolution is idempotent —
    // transformerOf hands back the level's one attachment instance — so a race can only cost a repeated lookup, never a
    // second transformer.
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
