package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.toroidalworld.compat.create.CreateSeamFold;
import com.toroidalworld.core.FoldedBoxQuery;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Mixin(targets = "com.simibubi.create.content.contraptions.ContraptionCollider", remap = false)
public class ContraptionColliderMixin {
    @ModifyArg(method = "getWorldToLocalTranslation",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/contraptions/ContraptionCollider;worldToLocalPos(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lcom/simibubi/create/foundation/collision/Matrix3d;F)Lnet/minecraft/world/phys/Vec3;"),
            index = 0)
    private static Vec3 toroidal$entityInTheAnchorFrame(Vec3 position, @Local(argsOnly = true) Entity entity,
            @Local(argsOnly = true) Vec3 anchorVec) {
        return CreateSeamFold.nearestCopy(entity.level(), anchorVec, position);
    }

    @ModifyExpressionValue(method = "collideBlocks",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/contraptions/ControlledContraptionEntity;getBoundingBox()Lnet/minecraft/world/phys/AABB;"))
    private static AABB toroidal$otherContraptionBoxInThisFrame(AABB otherBounds, @Local(name = "world") Level level,
            @Local(name = "position") Vec3 position) {
        return FoldedBoxQuery.toward(WorldLoopAttachments.wrappedTransformerOfReader(level), position, otherBounds);
    }

    @ModifyExpressionValue(method = "collideBlocks",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/contraptions/ControlledContraptionEntity;position()Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 toroidal$otherContraptionPositionInThisFrame(Vec3 otherPosition,
            @Local(name = "world") Level level, @Local(name = "position") Vec3 position) {
        return CreateSeamFold.nearestCopy(level, position, otherPosition);
    }
}
