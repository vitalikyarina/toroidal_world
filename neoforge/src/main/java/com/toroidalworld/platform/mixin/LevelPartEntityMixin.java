package com.toroidalworld.platform.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.InjectionTargets;
import com.toroidalworld.core.WorldLoopAttachments;
import com.toroidalworld.engine.fold.FoldedBoxQuery;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

@Mixin(Level.class)
public class LevelPartEntityMixin {
    @WrapOperation(
            method = "getEntities(Lnet/minecraft/world/level/entity/EntityTypeTest;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;Ljava/util/List;I)V",
            at = @At(value = "INVOKE", target = InjectionTargets.AABB_INTERSECTS))
    private boolean toroidal$typedPartBoxTowardQuery(AABB part, AABB query, Operation<Boolean> original) {
        return original.call(toroidal$seatTowardQuery(part, query), query);
    }

    @WrapOperation(
            method = "hasEntities(Lnet/minecraft/world/level/entity/EntityTypeTest;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Z",
            at = @At(value = "INVOKE", target = InjectionTargets.AABB_INTERSECTS))
    private boolean toroidal$hasPartBoxTowardQuery(AABB part, AABB query, Operation<Boolean> original) {
        return original.call(toroidal$seatTowardQuery(part, query), query);
    }

    @Unique
    private AABB toroidal$seatTowardQuery(AABB part, AABB query) {
        return FoldedBoxQuery.toward(WorldLoopAttachments.transformerOf((Level) (Object) this), query.getCenter(), part);
    }
}
