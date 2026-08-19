package com.toroidalworld.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldLoopTransformer;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Mixin(Mob.class)
public class MobMixin {
    @ModifyVariable(method = "lookAt(Lnet/minecraft/world/entity/Entity;FF)V", at = @At("STORE"), ordinal = 0)
    private double toroidal$lookDeltaX(double deltaX) {
        WorldLoopTransformer transformer = ((TransformerSource) this).toroidal$wrappedTransformer();
        return transformer == null ? deltaX : transformer.coords.x.foldDelta(deltaX);
    }

    @ModifyVariable(method = "lookAt(Lnet/minecraft/world/entity/Entity;FF)V", at = @At("STORE"), ordinal = 1)
    private double toroidal$lookDeltaZ(double deltaZ) {
        WorldLoopTransformer transformer = ((TransformerSource) this).toroidal$wrappedTransformer();
        return transformer == null ? deltaZ : transformer.coords.z.foldDelta(deltaZ);
    }

    @ModifyExpressionValue(
            method = "isWithinMeleeAttackRange",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getHitbox()Lnet/minecraft/world/phys/AABB;"))
    private AABB toroidal$meleeHitboxThroughSeam(AABB hitbox) {
        WorldLoopTransformer transformer = ((TransformerSource) this).toroidal$wrappedTransformer();
        return transformer == null ? hitbox : transformer.foldBoxToward(((Mob) (Object) this).position(), hitbox);
    }

    @ModifyExpressionValue(
            method = "isWithinHome(Lnet/minecraft/core/BlockPos;)Z",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/world/entity/Mob;homePosition:Lnet/minecraft/core/BlockPos;",
                    opcode = Opcodes.GETFIELD))
    private BlockPos toroidal$homeThroughSeam(BlockPos home, @Local(argsOnly = true) BlockPos pos) {
        return toroidal$nearestHome(home, pos);
    }

    @ModifyExpressionValue(
            method = "isWithinHome(Lnet/minecraft/world/phys/Vec3;)Z",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/world/entity/Mob;homePosition:Lnet/minecraft/core/BlockPos;",
                    opcode = Opcodes.GETFIELD))
    private BlockPos toroidal$homeVecThroughSeam(BlockPos home, @Local(argsOnly = true) Vec3 pos) {
        return toroidal$nearestHome(home, BlockPos.containing(pos));
    }

    @Unique
    private BlockPos toroidal$nearestHome(BlockPos home, BlockPos anchor) {
        WorldLoopTransformer transformer = ((TransformerSource) this).toroidal$wrappedTransformer();
        return transformer == null ? home : transformer.blocks.nearestCopy(anchor, home);
    }
}
