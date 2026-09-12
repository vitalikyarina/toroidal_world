package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.InjectionTargets;
import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.engine.fold.FoldedBoxQuery;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

@Mixin(EntityGetter.class)
public interface EntityGetterMixin {
    @WrapOperation(
            method = "getEntityCollisions(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;",
            at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_GET_BOUNDING_BOX))
    private static AABB toroidal$collisionBoxThroughSeam(Entity found, Operation<AABB> original,
            @Local(argsOnly = true) AABB testArea) {
        return FoldedBoxQuery.toward(((TransformerSource) found).toroidal$wrappedTransformer(), testArea.getCenter(),
                original.call(found));
    }

    @WrapOperation(
            method = "isUnobstructed(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/shapes/VoxelShape;)Z",
            at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_GET_BOUNDING_BOX))
    private static AABB toroidal$obstructionBoxThroughSeam(Entity found, Operation<AABB> original,
            @Local(argsOnly = true) VoxelShape shape) {
        return FoldedBoxQuery.toward(((TransformerSource) found).toroidal$wrappedTransformer(),
                shape.bounds().getCenter(), original.call(found));
    }
}
