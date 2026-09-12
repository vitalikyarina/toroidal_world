package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.engine.fold.FoldedBoxQuery;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.phys.AABB;

@Mixin(targets = "net.minecraft.world.entity.monster.Phantom$PhantomSweepAttackGoal")
public class PhantomSweepAttackGoalMixin {
    // Two spellings of the same synthetic field: the NeoForge build reads Mojmap, where it stays this$0, while the
    // Fabric one runs on intermediary, which names every field — synthetic ones included — field_<n>.
    @Shadow(aliases = {"this$0", "field_7333"})
    @Final
    private Phantom phantom;

    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/AABB;intersects(Lnet/minecraft/world/phys/AABB;)Z"))
    private boolean toroidal$touchThroughSeam(AABB reach, AABB targetBox, Operation<Boolean> original) {
        WorldFold transformer = ((TransformerSource) this.phantom).toroidal$wrappedTransformer();
        AABB folded = FoldedBoxQuery.toward(transformer, this.phantom.position(), targetBox);
        return original.call(reach, folded);
    }
}
