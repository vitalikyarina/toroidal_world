package com.toroidalworld.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.FoldedBoxQuery;
import com.toroidalworld.core.WorldFold;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Mixin(Mob.class)
public class MobMixin {
    @ModifyVariable(method = "lookAt(Lnet/minecraft/world/entity/Entity;FF)V", at = @At("STORE"), ordinal = 0)
    private double toroidal$lookDeltaX(double deltaX, @Local(argsOnly = true) Entity target) {
        WorldFold transformer = ((TransformerSource) this).toroidal$wrappedTransformer();
        return transformer == null ? deltaX : toroidal$deltaTo(transformer, target).x;
    }

    @ModifyVariable(method = "lookAt(Lnet/minecraft/world/entity/Entity;FF)V", at = @At("STORE"), ordinal = 1)
    private double toroidal$lookDeltaZ(double deltaZ, @Local(argsOnly = true) Entity target) {
        WorldFold transformer = ((TransformerSource) this).toroidal$wrappedTransformer();
        return transformer == null ? deltaZ : toroidal$deltaTo(transformer, target).z;
    }

    @ModifyExpressionValue(
            method = "isWithinMeleeAttackRange",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getHitbox()Lnet/minecraft/world/phys/AABB;"))
    private AABB toroidal$meleeHitboxThroughSeam(AABB hitbox) {
        WorldFold transformer = ((TransformerSource) this).toroidal$wrappedTransformer();
        return FoldedBoxQuery.toward(transformer, ((Mob) (Object) this).position(), hitbox);
    }

    @ModifyExpressionValue(
            method = "isWithinRestriction(Lnet/minecraft/core/BlockPos;)Z",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/world/entity/Mob;restrictCenter:Lnet/minecraft/core/BlockPos;",
                    opcode = Opcodes.GETFIELD))
    private BlockPos toroidal$homeThroughSeam(BlockPos home, @Local(argsOnly = true) BlockPos pos) {
        return toroidal$nearestHome(home, pos);
    }

    @Unique
    private Vec3 toroidal$deltaTo(WorldFold transformer, Entity target) {
        return transformer.foldDelta(((Mob) (Object) this).position(), target.position());
    }

    @Unique
    private BlockPos toroidal$nearestHome(BlockPos home, BlockPos anchor) {
        WorldFold transformer = ((TransformerSource) this).toroidal$wrappedTransformer();
        return transformer == null ? home : transformer.nearestCopy(anchor, home);
    }
}
