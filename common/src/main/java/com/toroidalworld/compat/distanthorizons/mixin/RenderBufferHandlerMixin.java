package com.toroidalworld.compat.distanthorizons.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.api.ToroidalShape;
import com.toroidalworld.compat.distanthorizons.DhClientShapes;
import com.toroidalworld.compat.distanthorizons.DhFold;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos2D;
import com.seibel.distanthorizons.core.render.QuadTree.LodQuadTree;
import com.seibel.distanthorizons.core.render.QuadTree.LodRenderSection;
import com.seibel.distanthorizons.core.render.RenderBufferHandler;

@Mixin(RenderBufferHandler.class)
public class RenderBufferHandlerMixin {
    @Shadow
    @Final
    public LodQuadTree lodQuadTree;

    @WrapOperation(
            method = "buildRenderList",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/seibel/distanthorizons/core/render/QuadTree/LodRenderSection;getRenderingEnabled()Z"))
    private boolean toroidal$drawOnlyTheNearestCopy(LodRenderSection section, Operation<Boolean> original) {
        if (!original.call(section)) {
            return false;
        }

        ToroidalShape shape = DhClientShapes.ofCurrentLevel();
        if (shape == null) {
            return true;
        }

        DhBlockPos2D center = this.lodQuadTree.getCenterBlockPos();
        int centerX = DhSectionPos.getCenterBlockPosX(section.pos);
        int centerZ = DhSectionPos.getCenterBlockPosZ(section.pos);
        return DhFold.isNearestCopy(shape, center.x, center.z, centerX, centerZ);
    }
}
