package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.InjectionTargets;
import com.toroidalworld.core.DimensionMapping;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;

@Mixin(CommandSourceStack.class)
public class CommandSourceStackMixin {
    @Shadow
    @Final
    private Vec3 worldPosition;

    @Shadow
    @Final
    private ServerLevel level;

    @Shadow
    @Final
    private EntityAnchorArgument.Anchor anchor;

    @WrapOperation(
            method = "withLevel",
            at = @At(value = "NEW", target = InjectionTargets.VEC3_NEW))
    private Vec3 toroidal$mapByWidthRatio(double x, double y, double z, Operation<Vec3> original,
            @Local(argsOnly = true) ServerLevel newLevel) {
        WorldFold destination = WorldLoopAttachments.wrappedTransformerOf(newLevel);
        WorldFold source = WorldLoopAttachments.wrappedTransformerOf(this.level);
        if (destination == null || source == null) {
            return original.call(x, y, z);
        }

        double declaredScale = DimensionType.getTeleportationScale(
                this.level.dimensionType(), newLevel.dimensionType());
        Vec3 mapped = DimensionMapping.map(source, destination, this.worldPosition, declaredScale);
        return original.call(mapped.x, y, mapped.z);
    }

    @ModifyVariable(
            method = "facing(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/commands/CommandSourceStack;",
            at = @At("HEAD"),
            argsOnly = true)
    private Vec3 toroidal$faceNearestCopy(Vec3 pos) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(this.level);
        if (transformer == null) {
            return pos;
        }

        Vec3 from = this.anchor.apply((CommandSourceStack) (Object) this);
        return transformer.nearestCopy(from, pos);
    }
}
