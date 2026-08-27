package com.toroidalworld.compat.sable.mixin;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.toroidalworld.compat.sable.SeamFrame;

import dev.ryanhcode.sable.util.SubLevelInclusiveLevelEntityGetter;

import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityTypeTest;
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
}
