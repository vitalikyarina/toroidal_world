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

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;

import xaero.lib.client.graphics.GpuTextureAndView;
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
    private double userScale;
    @Shadow
    private double cameraX;
    @Shadow
    private double cameraZ;
    @Shadow
    private static double destScale;

    @Shadow
    protected abstract double getScaleMultiplier(int screenShortSide);

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
    @Unique
    private final LongOpenHashSet toroidal$drawnCanonicalSlots = new LongOpenHashSet();
    @Unique
    private int toroidal$cursorLapX;
    @Unique
    private int toroidal$cursorLapZ;

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void toroidal$beginFrame(CallbackInfo ci) {
        this.toroidal$drawnCanonicalSlots.clear();
        double floor = toroidal$zoomFloor();
        if (floor > 0.0) {
            if (destScale < floor) {
                destScale = floor;
            }

            if (this.userScale < floor) {
                this.userScale = floor;
            }
        }
    }

    @Inject(method = "applyZoomLimits", at = @At("TAIL"))
    private void toroidal$floorZoomOut(CallbackInfo ci) {
        double floor = toroidal$zoomFloor();
        if (floor > 0.0 && destScale < floor) {
            destScale = floor;
        }
    }

    @Unique
    private double toroidal$zoomFloor() {
        Window window = Minecraft.getInstance().getWindow();
        return XaeroWorldMapFold.zoomFloorScale(this.getScaleMultiplier(Math.min(window.getWidth(), window.getHeight())));
    }

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
        int rawX = this.mouseBlockPosX;
        int rawZ = this.mouseBlockPosZ;
        this.mouseBlockPosX = XaeroWorldMapFold.foldBlock(Direction.Axis.X, rawX);
        this.mouseBlockPosZ = XaeroWorldMapFold.foldBlock(Direction.Axis.Z, rawZ);
        this.toroidal$cursorLapX = rawX - this.mouseBlockPosX;
        this.toroidal$cursorLapZ = rawZ - this.mouseBlockPosZ;
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
        if (original != null || !XaeroWorldMapFold.active()) {
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
        if (!XaeroWorldMapFold.active()) {
            return isCandidate ? null : region.getTexture(slotX, slotZ);
        }

        if (!XaeroWorldMapFold.glueableAt(slotSize)) {
            if (!isCandidate) {
                return region.getTexture(slotX, slotZ);
            }

            this.toroidal$slotFolded = true;
            return toroidal$anyCanonicalTexture(viewBlockX, viewBlockZ, slotSize);
        }

        int foldedBlockX = XaeroWorldMapFold.foldBlock(Direction.Axis.X, viewBlockX);
        int foldedBlockZ = XaeroWorldMapFold.foldBlock(Direction.Axis.Z, viewBlockZ);
        if (foldedBlockX == viewBlockX && foldedBlockZ == viewBlockZ) {
            return isCandidate ? null : region.getTexture(slotX, slotZ);
        }

        this.toroidal$slotFolded = true;
        return toroidal$canonicalRegionTexture(foldedBlockX, foldedBlockZ);
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
        Window window = Minecraft.getInstance().getWindow();
        int[] spanX = XaeroWorldMapFold.viewSpan(this.cameraX, window.getWidth(), this.scale, slotSize);
        int[] spanZ = XaeroWorldMapFold.viewSpan(this.cameraZ, window.getHeight(), this.scale, slotSize);
        int[] lapsX = copiesX.laps(spanX[0], spanX[1]);
        int[] lapsZ = copiesZ.laps(spanZ[0], spanZ[1]);
        int viewBlockX = this.toroidal$slotViewBlockX;
        int viewBlockZ = this.toroidal$slotViewBlockZ;
        for (int originX : toroidal$canonicalOrigins(Direction.Axis.X, copiesX, viewBlockX, slotSize)) {
            for (int originZ : toroidal$canonicalOrigins(Direction.Axis.Z, copiesZ, viewBlockZ, slotSize)) {
                if (!this.toroidal$drawnCanonicalSlots.add(((long) originX << 32) ^ (originZ & 0xFFFFFFFFL))) {
                    continue;
                }

                RegionTexture<?> regionTexture = toroidal$canonicalRegionTexture(originX, originZ);
                GpuTextureAndView textureAndView = regionTexture == null ? null : regionTexture.getGlColorTexture();
                if (textureAndView == null) {
                    continue;
                }

                int clippedMinX = copiesX.clipMin(originX);
                int clippedMaxX = copiesX.clipMax(originX + slotSize);
                int clippedMinZ = copiesZ.clipMin(originZ);
                int clippedMaxZ = copiesZ.clipMax(originZ + slotSize);
                if (clippedMinX >= clippedMaxX || clippedMinZ >= clippedMaxZ) {
                    continue;
                }

                float clippedX = x + (clippedMinX - viewBlockX);
                float clippedY = y + (clippedMinZ - viewBlockZ);
                float clippedWidth = clippedMaxX - clippedMinX;
                float clippedHeight = clippedMaxZ - clippedMinZ;
                float u1 = (float) (clippedMinX - originX) / slotSize;
                float u2 = (float) (clippedMaxX - originX) / slotSize;
                float v1 = (float) (clippedMinZ - originZ) / slotSize;
                float v2 = (float) (clippedMaxZ - originZ) / slotSize;
                for (int lapX : lapsX) {
                    for (int lapZ : lapsZ) {
                        float copyX = clippedX + copiesX.offset(lapX);
                        float copyY = clippedY + copiesZ.offset(lapZ);
                        BufferBuilder quad = renderer.begin(textureAndView.view);
                        quad.addVertex(matrix, copyX, copyY + clippedHeight, 0.0F).setUv(u1, v2);
                        quad.addVertex(matrix, copyX + clippedWidth, copyY + clippedHeight, 0.0F).setUv(u2, v2);
                        quad.addVertex(matrix, copyX + clippedWidth, copyY, 0.0F).setUv(u2, v1);
                        quad.addVertex(matrix, copyX, copyY, 0.0F).setUv(u1, v1);
                    }
                }
            }
        }
    }

    @Unique
    private static int[] toroidal$canonicalOrigins(Direction.Axis axis, AxisCopies copies, int viewBlock, int slotSize) {
        if (!copies.loops()) {
            return new int[] {viewBlock};
        }

        int firstOrigin = Math.floorDiv(XaeroWorldMapFold.foldBlock(axis, viewBlock), slotSize) * slotSize;
        int lastOrigin = Math.floorDiv(XaeroWorldMapFold.foldBlock(axis, viewBlock + slotSize - 1), slotSize) * slotSize;
        return firstOrigin == lastOrigin ? new int[] {firstOrigin} : new int[] {firstOrigin, lastOrigin};
    }

    @Unique
    private RegionTexture<?> toroidal$anyCanonicalTexture(int viewBlockX, int viewBlockZ, int slotSize) {
        AxisCopies copiesX = XaeroWorldMapFold.copies(Direction.Axis.X);
        AxisCopies copiesZ = XaeroWorldMapFold.copies(Direction.Axis.Z);
        for (int originX : toroidal$canonicalOrigins(Direction.Axis.X, copiesX, viewBlockX, slotSize)) {
            for (int originZ : toroidal$canonicalOrigins(Direction.Axis.Z, copiesZ, viewBlockZ, slotSize)) {
                RegionTexture<?> regionTexture = toroidal$canonicalRegionTexture(originX, originZ);
                if (regionTexture != null) {
                    return regionTexture;
                }
            }
        }

        return null;
    }

    @Unique
    private RegionTexture<?> toroidal$canonicalRegionTexture(int canonicalBlockX, int canonicalBlockZ) {
        int level = this.toroidal$viewLevel;
        int slotSize = 64 << level;
        int side = 512 << level;
        int canonicalRegX = Math.floorDiv(canonicalBlockX, side);
        int canonicalRegZ = Math.floorDiv(canonicalBlockZ, side);
        LeveledRegion<?> canonical = this.toroidal$processor
                .getLeveledRegion(this.toroidal$viewCaveLayer, canonicalRegX, canonicalRegZ, level);
        if (canonical == null || !canonical.hasTextures()) {
            return null;
        }

        return canonical.getTexture((canonicalBlockX - canonicalRegX * side) / slotSize,
                (canonicalBlockZ - canonicalRegZ * side) / slotSize);
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
        if (!XaeroWorldMapFold.active()) {
            original.call(matrixStack, overlayBuffer, flooredCameraX, flooredCameraZ, leftX, rightX, topZ, bottomZ,
                    sideR, sideG, sideB, sideA, centerR, centerG, centerB, centerA);
            return;
        }

        int lapX = this.toroidal$cursorLapX;
        int lapZ = this.toroidal$cursorLapZ;
        original.call(matrixStack, overlayBuffer, flooredCameraX, flooredCameraZ,
                leftX + lapX, rightX + lapX, topZ + lapZ, bottomZ + lapZ,
                sideR, sideG, sideB, sideA, centerR, centerG, centerB, centerA);

        AxisCopies copiesX = XaeroWorldMapFold.copies(Direction.Axis.X);
        AxisCopies copiesZ = XaeroWorldMapFold.copies(Direction.Axis.Z);
        int thickness = Math.max(1, (int) Math.ceil(1.0 / this.scale));
        Window window = Minecraft.getInstance().getWindow();
        int[] spanX = XaeroWorldMapFold.viewSpan(this.cameraX, window.getWidth(), this.scale, thickness);
        int[] spanZ = XaeroWorldMapFold.viewSpan(this.cameraZ, window.getHeight(), this.scale, thickness);
        int[] linesX = copiesX.seams(spanX[0], spanX[1]);
        int[] linesZ = copiesZ.seams(spanZ[0], spanZ[1]);
        Matrix4f matrix = matrixStack.last().pose();
        for (int lineX : linesX) {
            MapRenderHelper.fillIntoExistingBuffer(matrix, overlayBuffer,
                    lineX - flooredCameraX, spanZ[0] - flooredCameraZ,
                    lineX - flooredCameraX + thickness, spanZ[1] - flooredCameraZ,
                    1.0F, 1.0F, 1.0F, 0.8F);
        }

        for (int lineZ : linesZ) {
            MapRenderHelper.fillIntoExistingBuffer(matrix, overlayBuffer,
                    spanX[0] - flooredCameraX, lineZ - flooredCameraZ,
                    spanX[1] - flooredCameraX, lineZ - flooredCameraZ + thickness,
                    1.0F, 1.0F, 1.0F, 0.8F);
        }
    }
}
