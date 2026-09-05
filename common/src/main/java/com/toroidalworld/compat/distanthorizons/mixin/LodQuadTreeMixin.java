package com.toroidalworld.compat.distanthorizons.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.api.ToroidalShape;
import com.toroidalworld.compat.distanthorizons.DhClientShapes;
import com.toroidalworld.compat.distanthorizons.DhFold;
import com.toroidalworld.compat.distanthorizons.DhKeys;
import com.toroidalworld.compat.distanthorizons.DhShapes;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.seibel.distanthorizons.core.level.IDhClientLevel;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos2D;
import com.seibel.distanthorizons.core.render.QuadTree.LodQuadTree;
import com.seibel.distanthorizons.core.render.QuadTree.LodRenderSection;
import com.seibel.distanthorizons.core.render.QuadTree.QuadTreeTickNodeHolder;
import com.seibel.distanthorizons.core.util.objects.quadTree.QuadTree;

@Mixin(LodQuadTree.class)
public class LodQuadTreeMixin {
    @Shadow
    @Final
    private IDhClientLevel level;

    @WrapMethod(method = "queuePosToReload")
    private void toroidal$reloadTheNearestCopy(long pos, Operation<Void> original) {
        ToroidalShape shape = DhShapes.of(this.level);
        if (shape == null) {
            original.call(pos);
            return;
        }

        DhBlockPos2D center = ((QuadTree<?>) (Object) this).getCenterBlockPos();
        original.call(DhKeys.nearestSection(shape, center.x, center.z, pos));
    }

    @ModifyReturnValue(
            method = "calcExpectedDetailLevel(Lcom/seibel/distanthorizons/core/pos/blockPos/DhBlockPos2D;IID)B",
            at = @At("RETURN"))
    private byte toroidal$capDetailAtTheWorld(byte expected) {
        ToroidalShape shape = DhShapes.of(this.level);
        if (shape == null) {
            return expected;
        }

        byte cap = DhFold.maxExpectedDetailLevel(shape, DhSectionPos.SECTION_MINIMUM_DETAIL_LEVEL);
        return expected <= cap ? expected : cap;
    }

    @WrapMethod(method = "calcExpectedDetailLevel(Lcom/seibel/distanthorizons/core/pos/blockPos/DhBlockPos2D;J)B")
    private byte toroidal$splitAStraddler(DhBlockPos2D playerPos, long sectionPos, Operation<Byte> original) {
        byte expected = original.call(playerPos, sectionPos);
        ToroidalShape shape = DhShapes.of(this.level);
        if (shape == null) {
            return expected;
        }

        DhBlockPos2D center = ((QuadTree<?>) (Object) this).getCenterBlockPos();
        byte leaf = DhSectionPos.SECTION_MINIMUM_DETAIL_LEVEL;
        if (DhKeys.straddlesNearestCopy(shape, center.x, center.z, sectionPos)) {
            byte cap = (byte) (DhKeys.snapLevel(shape) - leaf);
            return expected <= cap ? expected : cap;
        }

        if (DhKeys.straddlesNearestCopy(shape, center.x, center.z, DhSectionPos.getParentPos(sectionPos))) {
            byte cap = (byte) (DhSectionPos.getDetailLevel(sectionPos) + 1 - leaf);
            return expected <= cap ? expected : cap;
        }

        return expected;
    }

    @WrapOperation(
            method = {"onDetailLevelTooLow", "onDesiredDetailLevel"},
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/seibel/distanthorizons/core/render/QuadTree/LodRenderSection;canRender()Z"))
    private boolean toroidal$refuseAnIncompleteSection(LodRenderSection section, Operation<Boolean> original) {
        boolean canRender = original.call(section);
        ToroidalShape shape = DhShapes.of(this.level);
        if (shape == null) {
            return canRender;
        }

        byte detailLevel = DhSectionPos.getDetailLevel(section.pos);
        int sectionX = DhSectionPos.getX(section.pos);
        int sectionZ = DhSectionPos.getZ(section.pos);
        DhBlockPos2D center = ((QuadTree<?>) (Object) this).getCenterBlockPos();
        return canRender
                && DhFold.isCompleteSection(shape, DhKeys.LEAF, detailLevel, sectionX, sectionZ)
                && DhKeys.isNearestCopy(shape, center.x, center.z, section.pos);
    }

    @WrapOperation(
            method = "recursivelyUpdateRenderSectionNode",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/seibel/distanthorizons/core/render/QuadTree/QuadTreeTickNodeHolder;"
                            + "addLoadSection(Lcom/seibel/distanthorizons/core/render/QuadTree/LodRenderSection;)V"))
    private void toroidal$loadOnlyTheNearestCopy(QuadTreeTickNodeHolder holder, LodRenderSection section,
            Operation<Void> original) {
        ToroidalShape shape = DhShapes.of(this.level);
        if (shape == null) {
            original.call(holder, section);
            return;
        }

        DhBlockPos2D center = ((QuadTree<?>) (Object) this).getCenterBlockPos();
        if (DhKeys.isNearestCopy(shape, center.x, center.z, section.pos)) {
            original.call(holder, section);
        }
    }

    @WrapOperation(
            method = "lambda$updateAllRenderSections$1",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/seibel/distanthorizons/core/pos/DhSectionPos;contains(JJ)Z"))
    private static boolean toroidal$cancelByACopyOfTheSection(long sectionPos, long genPos,
            Operation<Boolean> original) {
        ToroidalShape shape = DhClientShapes.ofCurrentLevel();
        return shape == null ? original.call(sectionPos, genPos) : DhKeys.containsACopy(shape, sectionPos, genPos);
    }
}
