package com.toroidalworld.compat.xaero.mixin.map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.joml.Matrix4f;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.toroidalworld.compat.AxisCopies;
import com.toroidalworld.compat.xaero.XaeroWorldMapFold;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;

import xaero.map.MapProcessor;
import xaero.map.graphics.MapRenderHelper;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRenderer;
import xaero.map.gui.GuiMap;
import xaero.map.region.LeveledRegion;
import xaero.map.region.texture.RegionTexture;

@Mixin(value = GuiMap.class, remap = false)
public abstract class GuiMapMixin {
    @Shadow
    private int mouseBlockPosX;
    @Shadow
    private int mouseBlockPosZ;
    @Shadow
    private double scale;
    @Shadow
    private double cameraX;
    @Shadow
    private double cameraZ;

    @Unique
    private MapProcessor toroidal$processor;
    @Unique
    private int toroidal$viewLeveledRegX;
    @Unique
    private int toroidal$viewLeveledRegZ;
    @Unique
    private int toroidal$viewLevel;
    @Unique
    private int toroidal$viewCaveLayer;
    @Unique
    private LeveledRegion<?> toroidal$leveledCandidate;
    @Unique
    private boolean toroidal$slotFolded;
    @Unique
    private int toroidal$slotViewBlockX;
    @Unique
    private int toroidal$slotViewBlockZ;
    @WrapOperation(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/entity/util/EntityUtil;getEntityX(Lnet/minecraft/world/entity/Entity;F)D"))
    private double toroidal$foldCameraX(Entity entity, float partialTicks, Operation<Double> original) {
        return XaeroWorldMapFold.foldCameraCoord(Direction.Axis.X, original.call(entity, partialTicks));
    }

    @WrapOperation(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/entity/util/EntityUtil;getEntityZ(Lnet/minecraft/world/entity/Entity;F)D"))
    private double toroidal$foldCameraZ(Entity entity, float partialTicks, Operation<Double> original) {
        return XaeroWorldMapFold.foldCameraCoord(Direction.Axis.Z, original.call(entity, partialTicks));
    }

    @Inject(
            method = "extractRenderState",
            at = @At(
                    value = "FIELD",
                    target = "Lxaero/map/gui/GuiMap;mouseBlockPosZ:I",
                    opcode = 181,
                    ordinal = 1,
                    shift = At.Shift.AFTER))
    private void toroidal$foldCursorBlockPos(CallbackInfo ci) {
        this.mouseBlockPosX = XaeroWorldMapFold.foldBlock(Direction.Axis.X, this.mouseBlockPosX);
        this.mouseBlockPosZ = XaeroWorldMapFold.foldBlock(Direction.Axis.Z, this.mouseBlockPosZ);
    }

    @Redirect(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/MapProcessor;getLeveledRegion(IIII)Lxaero/map/region/LeveledRegion;"))
    private LeveledRegion<?> toroidal$fetchLeveledRegion(MapProcessor processor, int caveLayer, int regX, int regZ, int level) {
        this.toroidal$processor = processor;
        this.toroidal$viewLeveledRegX = regX;
        this.toroidal$viewLeveledRegZ = regZ;
        this.toroidal$viewLevel = level;
        this.toroidal$viewCaveLayer = caveLayer;
        this.toroidal$leveledCandidate = null;
        LeveledRegion<?> original = processor.getLeveledRegion(caveLayer, regX, regZ, level);
        if (original != null || !XaeroWorldMapFold.active() || !XaeroWorldMapFold.glueableAt(64 << level)) {
            return original;
        }

        // A candidate value only, so the draw block runs at all; the texture redirect re-resolves each slot precisely.
        int side = 512 << level;
        int foldedOriginX = XaeroWorldMapFold.foldBlock(Direction.Axis.X, regX * side);
        int foldedOriginZ = XaeroWorldMapFold.foldBlock(Direction.Axis.Z, regZ * side);
        LeveledRegion<?> candidate = processor.getLeveledRegion(
                caveLayer, Math.floorDiv(foldedOriginX, side), Math.floorDiv(foldedOriginZ, side), level);
        this.toroidal$leveledCandidate = candidate;
        return candidate;
    }

    // An origin-fold substitute, so the block runs even where the cell has no LEAF region of its own.
    @Redirect(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/MapProcessor;getLeafMapRegion(IIIZ)Lxaero/map/region/MapRegion;"))
    private xaero.map.region.MapRegion toroidal$fetchLeafRegion(MapProcessor processor, int caveLayer, int regX, int regZ, boolean create) {
        xaero.map.region.MapRegion original = processor.getLeafMapRegion(caveLayer, regX, regZ, create);
        if (original != null || !XaeroWorldMapFold.active()) {
            return original;
        }

        int foldedOriginX = XaeroWorldMapFold.foldBlock(Direction.Axis.X, regX * 512);
        int foldedOriginZ = XaeroWorldMapFold.foldBlock(Direction.Axis.Z, regZ * 512);
        return processor.getLeafMapRegion(
                caveLayer, Math.floorDiv(foldedOriginX, 512), Math.floorDiv(foldedOriginZ, 512), false);
    }

    @Redirect(
            method = "extractRenderState",
            at = @At(value = "INVOKE", target = "Lxaero/map/region/LeveledRegion;hasTextures()Z"))
    private boolean toroidal$candidateHasTextures(LeveledRegion<?> region) {
        if (XaeroWorldMapFold.active() && region != null && region == this.toroidal$leveledCandidate) {
            return true;
        }

        return region.hasTextures();
    }

    @Redirect(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/region/LeveledRegion;getTexture(II)Lxaero/map/region/texture/RegionTexture;",
                    ordinal = 1))
    private RegionTexture<?> toroidal$foldLeafTexture(LeveledRegion<?> region, int slotX, int slotZ) {
        this.toroidal$slotFolded = false;
        boolean isCandidate = region == this.toroidal$leveledCandidate;
        int level = this.toroidal$viewLevel;
        int slotSize = 64 << level;
        int side = 512 << level;
        int viewBlockX = this.toroidal$viewLeveledRegX * side + slotX * slotSize;
        int viewBlockZ = this.toroidal$viewLeveledRegZ * side + slotZ * slotSize;
        this.toroidal$slotViewBlockX = viewBlockX;
        this.toroidal$slotViewBlockZ = viewBlockZ;
        if (!XaeroWorldMapFold.active() || !XaeroWorldMapFold.glueableAt(slotSize)) {
            return isCandidate ? null : region.getTexture(slotX, slotZ);
        }

        int foldedBlockX = XaeroWorldMapFold.foldBlock(Direction.Axis.X, viewBlockX);
        int foldedBlockZ = XaeroWorldMapFold.foldBlock(Direction.Axis.Z, viewBlockZ);
        if (foldedBlockX == viewBlockX && foldedBlockZ == viewBlockZ) {
            return isCandidate ? null : region.getTexture(slotX, slotZ);
        }

        this.toroidal$slotFolded = true;
        if (!XaeroWorldMapFold.withinOnePeriod(viewBlockX, foldedBlockX, viewBlockZ, foldedBlockZ)) {
            return null;
        }

        int canonicalRegX = Math.floorDiv(foldedBlockX, side);
        int canonicalRegZ = Math.floorDiv(foldedBlockZ, side);
        LeveledRegion<?> canonical = this.toroidal$processor
                .getLeveledRegion(this.toroidal$viewCaveLayer, canonicalRegX, canonicalRegZ, level);
        if (canonical == null || !canonical.hasTextures()) {
            return null;
        }

        return canonical.getTexture((foldedBlockX - canonicalRegX * side) / slotSize, (foldedBlockZ - canonicalRegZ * side) / slotSize);
    }

    @Redirect(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/region/LeveledRegion;getTexture(II)Lxaero/map/region/texture/RegionTexture;",
                    ordinal = 2))
    private RegionTexture<?> toroidal$suppressFoldedRootTexture(LeveledRegion<?> region, int textureX, int textureZ) {
        if (XaeroWorldMapFold.active() && this.toroidal$slotFolded) {
            return null;
        }

        return region.getTexture(textureX, textureZ);
    }

    @WrapOperation(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/gui/GuiMap;renderTexturedModalRectWithLighting3(Lorg/joml/Matrix4f;FFFFLcom/mojang/blaze3d/textures/GpuTextureView;ZLxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRenderer;)V"))
    private void toroidal$drawClippedPeriodCopies(
            Matrix4f matrix, float x, float y, float width, float height,
            GpuTextureView texture, boolean hasLight, MultiTextureRenderTypeRenderer renderer, Operation<Void> original) {
        int slotSize = 64 << this.toroidal$viewLevel;
        if (!XaeroWorldMapFold.active() || XaeroWorldMapFold.glueableAt(slotSize)) {
            original.call(matrix, x, y, width, height, texture, hasLight, renderer);
            return;
        }

        AxisCopies copiesX = XaeroWorldMapFold.copies(Direction.Axis.X);
        AxisCopies copiesZ = XaeroWorldMapFold.copies(Direction.Axis.Z);
        int slotMinX = this.toroidal$slotViewBlockX;
        int slotMinZ = this.toroidal$slotViewBlockZ;
        int clippedMinX = copiesX.clipMin(slotMinX);
        int clippedMaxX = copiesX.clipMax(slotMinX + slotSize);
        int clippedMinZ = copiesZ.clipMin(slotMinZ);
        int clippedMaxZ = copiesZ.clipMax(slotMinZ + slotSize);
        XaeroWorldMapFold.logClipCopies(copiesX, copiesZ, slotSize, slotMinX, slotMinZ,
                clippedMinX, clippedMaxX, clippedMinZ, clippedMaxZ);
        if (clippedMinX >= clippedMaxX || clippedMinZ >= clippedMaxZ) {
            return;
        }

        float clippedX = x + (clippedMinX - slotMinX);
        float clippedY = y + (clippedMinZ - slotMinZ);
        float clippedWidth = clippedMaxX - clippedMinX;
        float clippedHeight = clippedMaxZ - clippedMinZ;
        float u1 = (float) (clippedMinX - slotMinX) / slotSize;
        float u2 = (float) (clippedMaxX - slotMinX) / slotSize;
        float v1 = (float) (clippedMinZ - slotMinZ) / slotSize;
        float v2 = (float) (clippedMaxZ - slotMinZ) / slotSize;
        // The quad is emitted directly: calling GuiMap's own helper would drag its xaerolib superclass onto the compile classpath.
        for (int lapX : copiesX.laps()) {
            for (int lapZ : copiesZ.laps()) {
                float copyX = clippedX + copiesX.offset(lapX);
                float copyY = clippedY + copiesZ.offset(lapZ);
                BufferBuilder quad = renderer.begin(texture);
                quad.addVertex(matrix, copyX, copyY + clippedHeight, 0.0F).setUv(u1, v2);
                quad.addVertex(matrix, copyX + clippedWidth, copyY + clippedHeight, 0.0F).setUv(u2, v2);
                quad.addVertex(matrix, copyX + clippedWidth, copyY, 0.0F).setUv(u2, v1);
                quad.addVertex(matrix, copyX, copyY, 0.0F).setUv(u1, v1);
            }
        }
    }

    @WrapOperation(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/graphics/MapRenderHelper;renderDynamicHighlight(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIIIIIFFFFFFFF)V",
                    ordinal = 0))
    private void toroidal$drawSeamGrid(
            PoseStack matrixStack, VertexConsumer overlayBuffer, int flooredCameraX, int flooredCameraZ,
            int leftX, int rightX, int topZ, int bottomZ,
            float sideR, float sideG, float sideB, float sideA, float centerR, float centerG, float centerB, float centerA,
            Operation<Void> original) {
        original.call(matrixStack, overlayBuffer, flooredCameraX, flooredCameraZ, leftX, rightX, topZ, bottomZ,
                sideR, sideG, sideB, sideA, centerR, centerG, centerB, centerA);
        if (!XaeroWorldMapFold.active()) {
            return;
        }

        AxisCopies copiesX = XaeroWorldMapFold.copies(Direction.Axis.X);
        AxisCopies copiesZ = XaeroWorldMapFold.copies(Direction.Axis.Z);
        int thickness = Math.max(1, (int) Math.ceil(1.0 / this.scale));
        Window window = Minecraft.getInstance().getWindow();
        int[] extentX = XaeroWorldMapFold.gridExtent(copiesX, this.cameraX, window.getWidth(), this.scale, thickness);
        int[] extentZ = XaeroWorldMapFold.gridExtent(copiesZ, this.cameraZ, window.getHeight(), this.scale, thickness);
        XaeroWorldMapFold.logSeamGrid(copiesX, copiesZ, extentX, extentZ);
        Matrix4f matrix = matrixStack.last().pose();
        for (int lineX : XaeroWorldMapFold.gridLines(copiesX)) {
            MapRenderHelper.fillIntoExistingBuffer(matrix, overlayBuffer,
                    lineX - flooredCameraX, extentZ[0] - flooredCameraZ,
                    lineX - flooredCameraX + thickness, extentZ[1] - flooredCameraZ,
                    1.0F, 1.0F, 1.0F, 0.8F);
        }

        for (int lineZ : XaeroWorldMapFold.gridLines(copiesZ)) {
            MapRenderHelper.fillIntoExistingBuffer(matrix, overlayBuffer,
                    extentX[0] - flooredCameraX, lineZ - flooredCameraZ,
                    extentX[1] - flooredCameraX, lineZ - flooredCameraZ + thickness,
                    1.0F, 1.0F, 1.0F, 0.8F);
        }
    }
}
