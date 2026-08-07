package com.toroidalworld.mixin;

import java.util.function.Function;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.command.SeamCommandErrors;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// The dx/dy/dz box is checked twice over: once by the level, which already cuts it at the seam and hands back the
// entities standing on the ground it really covers, and once more by this predicate — raw, so it throws away exactly
// those. The two disagree only near a seam, and the predicate has the last word.
//
// The check lives in a lambda inside getPredicate, where a handler scoped to the method matches nothing. Rather than
// name the lambda — its number moves whenever vanilla is recompiled — the method is asked to build its predicate with
// no box at all, which is a case it already supports, and the seam-aware test is added to what comes back. Nothing is
// restated: the feature and range filters are still vanilla's own.
//
// A box folded to the copy nearest the entity is what "inside the region" means on a torus. distance= needs no such
// help — it goes through Entity.distanceToSqr, which is already folded.
//
// Appending the test moves it behind the range filter, where vanilla ran it in front. That costs something only for a
// selector carrying dx= and distance= at once — two ways of saying the same thing, rarely written together — and only
// as far as a few multiplications per candidate. Putting it back in its place would mean shadowing the private range
// field and restating how vanilla composes its predicate, which is a new thing to get wrong at every Minecraft update.
// The order stands as the cheaper of the two.
@Mixin(EntitySelector.class)
public class EntitySelectorMixin {
    @WrapMethod(method = "getPredicate")
    private Predicate<Entity> toroidal$boxThroughSeam(Vec3 pos, @Nullable AABB absoluteAabb,
            @Nullable FeatureFlagSet enabledFeatures, Operation<Predicate<Entity>> original) {
        if (absoluteAabb == null) {
            return original.call(pos, absoluteAabb, enabledFeatures);
        }

        Predicate<Entity> withoutBox = original.call(pos, null, enabledFeatures);
        return entity -> withoutBox.test(entity) && toroidal$insideThroughSeam(entity, absoluteAabb);
    }

    // x=/y=/z= is a coordinate someone typed out, so it answers to the same rule as one typed into /tp: it must name a
    // place the world has. The parsed form is long gone by here — the values are sealed inside a position function —
    // but the function only ever replaces the axes that were given, so an axis that comes back different from the
    // sender's own is exactly an axis that was typed. Comparing that way also keeps an entity caught mid-step past the
    // bounds from being mistaken for a typed coordinate.
    @WrapOperation(
            method = {"findEntities", "findPlayers"},
            at = @At(value = "INVOKE", target = "Ljava/util/function/Function;apply(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object toroidal$refuseCentreOutsideWorld(Function<Object, Object> position, Object senderPosition,
            Operation<Object> original, @Local(argsOnly = true) CommandSourceStack sender)
            throws CommandSyntaxException {
        Object resolved = original.call(position, senderPosition);
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(sender.getLevel());
        if (transformer == null || !(resolved instanceof Vec3 pos) || !(senderPosition instanceof Vec3 senderPos)) {
            return resolved;
        }

        if (pos.x != senderPos.x) {
            SeamCommandErrors.requireInsideWorld(transformer.coords.x, pos.x);
        }

        if (pos.z != senderPos.z) {
            SeamCommandErrors.requireInsideWorld(transformer.coords.z, pos.z);
        }

        return resolved;
    }

    // The transformer is fetched per entity because getPredicate is handed no level to fetch it from, and the predicate
    // it builds is applied wherever the caller pleases. It could be fetched once instead: a box only exists when dx/dy/dz
    // was given, every one of which marks the selector world-limited, so the search never leaves the sender's level and
    // every entity reaching here shares one transformer — a field set on the first call would serve the rest.
    //
    // Left as a lookup on purpose. The saving is the map lookup itself: on a dx=200 selector matching some five hundred
    // entities it comes to about fifteen microseconds, three hundredths of one tick. The cost is a stateful predicate
    // where vanilla builds pure ones, on the unwritten assumption that these calls stay on one thread — an assumption
    // that would fail silently, as a wrong answer rather than a crash. Worth revisiting only if a profile ever puts
    // selectors somewhere near the top; the paragraph above is the whole design of that change.
    private static boolean toroidal$insideThroughSeam(Entity entity, AABB absoluteAabb) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(entity.level());
        AABB box = transformer == null ? absoluteAabb : transformer.foldBoxToward(entity.position(), absoluteAabb);
        return box.intersects(entity.getBoundingBox());
    }
}
