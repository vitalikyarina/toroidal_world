package com.toroidalworld.compat.sable.mixin;

import java.util.List;
import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.toroidalworld.compat.sable.SableEntityQuery;
import com.toroidalworld.compat.sable.SeamFrame;
import com.toroidalworld.core.FoldedBoxQuery;

import dev.ryanhcode.sable.util.SubLevelInclusiveLevelEntityGetter;

import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Mixin(SubLevelInclusiveLevelEntityGetter.class)
public abstract class SubLevelInclusiveLevelEntityGetterMixin<T extends EntityAccess> {
    @Shadow
    @Final
    private Level level;

    @WrapMethod(method = "get(Lnet/minecraft/world/phys/AABB;Ljava/util/function/Consumer;)V")
    private void toroidal$frameOnBoxQuery(AABB box, Consumer<T> consumer, Operation<Void> original) {
        Vec3 centre = box.getCenter();
        SeamFrame.run(this.level, () -> centre, () -> original.call(box, consumer));
    }

    @WrapMethod(method = "get(Lnet/minecraft/world/level/entity/EntityTypeTest;Lnet/minecraft/world/phys/AABB;Lnet/minecraft/util/AbortableIterationConsumer;)V")
    private <U extends T> void toroidal$frameOnTypedBoxQuery(EntityTypeTest<T, U> typeTest, AABB box,
            AbortableIterationConsumer<U> consumer, Operation<Void> original) {
        Vec3 centre = box.getCenter();
        SeamFrame.run(this.level, () -> centre, () -> original.call(typeTest, box, consumer));
    }

    @WrapOperation(
            method = "get(Lnet/minecraft/world/phys/AABB;Ljava/util/function/Consumer;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/entity/LevelEntityGetter;get(Lnet/minecraft/world/phys/AABB;Ljava/util/function/Consumer;)V"))
    private void toroidal$delegateThroughSeam(LevelEntityGetter<T> delegate, AABB box, Consumer<T> consumer,
            Operation<Void> original) {
        List<AABB> pieces = SableEntityQuery.pieces(this.level, box);
        if (pieces.size() == 1) {
            original.call(delegate, pieces.getFirst(), consumer);
            return;
        }

        Consumer<T> once = FoldedBoxQuery.deduplicating(consumer);
        for (AABB piece : pieces) {
            original.call(delegate, piece, once);
        }
    }

    @WrapOperation(
            method = "get(Lnet/minecraft/world/level/entity/EntityTypeTest;Lnet/minecraft/world/phys/AABB;Lnet/minecraft/util/AbortableIterationConsumer;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/entity/LevelEntityGetter;get(Lnet/minecraft/world/level/entity/EntityTypeTest;Lnet/minecraft/world/phys/AABB;Lnet/minecraft/util/AbortableIterationConsumer;)V"))
    private <U extends T> void toroidal$typedDelegateThroughSeam(LevelEntityGetter<T> delegate,
            EntityTypeTest<T, U> typeTest, AABB box, AbortableIterationConsumer<U> consumer,
            Operation<Void> original) {
        List<AABB> pieces = SableEntityQuery.pieces(this.level, box);
        if (pieces.size() == 1) {
            original.call(delegate, typeTest, pieces.getFirst(), consumer);
            return;
        }

        AbortableIterationConsumer<U> once = FoldedBoxQuery.deduplicating(consumer);
        for (AABB piece : pieces) {
            original.call(delegate, typeTest, piece, once);
        }
    }
}
