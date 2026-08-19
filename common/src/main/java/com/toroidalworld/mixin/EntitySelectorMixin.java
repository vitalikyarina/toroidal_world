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

    private static boolean toroidal$insideThroughSeam(Entity entity, AABB absoluteAabb) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(entity.level());
        AABB box = transformer == null ? absoluteAabb : transformer.foldBoxToward(entity.position(), absoluteAabb);
        return box.intersects(entity.getBoundingBox());
    }
}
