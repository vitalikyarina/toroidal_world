package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Illusioner;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Drowned;

// Six shooters, one arithmetic: read the target's absolute position, subtract the shooter's own, derive the flight
// time's worth of lift from the horizontal gap between them. Written out once per shooter because vanilla has no
// primitive for it — which is why the fold goes on the reading of the target rather than on any of the numbers built
// from it.
//
// The bow, the trident, the snowball and the spit all leave through the same call; the potion adds a lead on the
// target's own motion, which is a step from the target's position and therefore folds with it. The wither reads its
// target in the private (int, LivingEntity) overload — the skull leaves from a head, but a head is three blocks from
// the body: the copy nearest the wither is the copy nearest its heads. Its loose-coordinate overload sprays random
// points beside the shooter and has no target to read, so it needs no fold. Only the target's own coordinates are
// touched: the shooter reads its own through its concrete type, which this does not name.
//
// The method is named without a descriptor on purpose: the five mobs implement performRangedAttack(LivingEntity, float)
// while the wither reads the target in performRangedAttack(int, LivingEntity), and an explicit descriptor list cannot
// apply to targets that carry only one of the two. The bare name matches each class's own overloads, and only the one
// that reads the target contributes injection points; a class where none matched would still fail loudly at apply.
@Mixin({AbstractSkeleton.class, Illusioner.class, Drowned.class, SnowGolem.class, Witch.class, WitherBoss.class})
public class RangedAttackAimMixin {
    @ModifyExpressionValue(
            method = "performRangedAttack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getX()D"))
    private double toroidal$aimTargetX(double targetX) {
        return SeamAim.nearX((Entity) (Object) this, targetX);
    }

    @ModifyExpressionValue(
            method = "performRangedAttack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getZ()D"))
    private double toroidal$aimTargetZ(double targetZ) {
        return SeamAim.nearZ((Entity) (Object) this, targetZ);
    }
}
