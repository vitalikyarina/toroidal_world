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
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.toroidalworld.compat.xaero.XaeroWorldMapFold;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;

import xaero.map.MapProcessor;
import xaero.map.graphics.MapRenderHelper;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRenderer;
import xaero.map.gui.GuiMap;
import xaero.map.region.LeveledRegion;
import xaero.map.region.texture.RegionTexture;

// Two concerns on the full-map screen:
//
// 1. The camera-follow anchor reads the raw player position, which runs a world width per lap — with canonical
//    storage the map would open on empty ground a world away from where the terrain actually is. The two position
//    reads feeding the follow anchor (and the player arrow drawn from the same locals) fold canonical; the
//    free-pan branch never touches these reads, so a camera panned by hand is left alone. The fold happens before
//    the dimension scaling — the player position folds in the player's own level space, the only one the shape
//    describes.
//
// 2. The view glue: the region draw loop enumerates leveled regions straight off the view window, so past the
//    canonical edge there is nothing to draw. As on the minimap, the SOURCE folds and the PLACEMENT stays the
//    view slot: a null region fetch beyond the edge substitutes a canonical candidate (so the draw block runs at
//    all — its hasTextures answers yes by force), and the per-slot texture fetch re-resolves the exact canonical
//    texture from the slot's view-space block position. The root-texture fallback (the blurry placeholder) is
//    suppressed for folded slots rather than folded — its sub-rect math lives in locals out of reach, and a wrong
//    quadrant is worse than a briefly blank tile. Slots stay unglued at a zoom whose texture size the world width
//    does not divide (the nether at the deepest zoom-out). The redirects share per-iteration state; safe because
//    the render thread walks fetch → hasTextures → texture strictly in order.
// The injection method is GuiMap's override of Screen.render, and its NAME differs per loader jar: Mojmap "render"
// in the neoforge build, intermediary "method_25394" in the fabric build (the remap pipeline rewrites descriptors in
// the target strings but cannot rename an override it can't resolve to Screen). Both names are listed on every
// injector and defaultRequire=1 accepts whichever the running loader has — the same dual-name pattern as
// EntitySectionManagerMixin's addEntity/addEntityWithoutEvent.
@Mixin(GuiMap.class)
public abstract class GuiMapMixin {
    @Shadow
    private int mouseBlockPosX;
    @Shadow
    private int mouseBlockPosZ;
    @Shadow
    private double scale;

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
            method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"},
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/entity/util/EntityUtil;getEntityX(Lnet/minecraft/world/entity/Entity;F)D"))
    private double toroidal$foldCameraX(Entity entity, float partialTicks, Operation<Double> original) {
        return XaeroWorldMapFold.foldCameraCoord(Direction.Axis.X, original.call(entity, partialTicks));
    }

    @WrapOperation(
            method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"},
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/entity/util/EntityUtil;getEntityZ(Lnet/minecraft/world/entity/Entity;F)D"))
    private double toroidal$foldCameraZ(Entity entity, float partialTicks, Operation<Double> original) {
        return XaeroWorldMapFold.foldCameraCoord(Direction.Axis.Z, original.call(entity, partialTicks));
    }

    // The cursor block position, folded canonical right after it is derived from the view — everything downstream
    // (the coordinate readout, right-click menu, teleport, tile selection, the hover region lookups) then speaks
    // canonical coordinates, which is also where the glued copies' content actually lives.
    @Inject(
            method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"},
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
            method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"},
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

        // A candidate so the draw block runs at all; the texture redirect re-resolves each slot precisely. No
        // period cap here: a region straddling the 3x3 boundary must still run for its inner half — the per-slot
        // fold enforces the cap exactly.
        int side = 512 << level;
        int foldedOriginX = XaeroWorldMapFold.foldBlock(Direction.Axis.X, regX * side);
        int foldedOriginZ = XaeroWorldMapFold.foldBlock(Direction.Axis.Z, regZ * side);
        LeveledRegion<?> candidate = processor.getLeveledRegion(
                caveLayer, Math.floorDiv(foldedOriginX, side), Math.floorDiv(foldedOriginZ, side), level);
        this.toroidal$leveledCandidate = candidate;
        return candidate;
    }

    // The draw block additionally demands a non-null LEAF region per 512-block cell — for a cell fully beyond the
    // canonical edge there is none, and the whole block (including the per-slot texture folds) never runs. An
    // origin-fold substitute is enough: the block runs, the canonical region rides the reload queue, and the
    // texture redirect resolves each slot precisely anyway.
    @Redirect(
            method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"},
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/MapProcessor;getLeafMapRegion(IIIZ)Lxaero/map/region/MapRegion;"))
    private xaero.map.region.MapRegion toroidal$fetchLeafRegion(MapProcessor processor, int caveLayer, int regX, int regZ, boolean create) {
        xaero.map.region.MapRegion original = processor.getLeafMapRegion(caveLayer, regX, regZ, create);
        if (original != null || !XaeroWorldMapFold.active()) {
            return original;
        }

        // No period cap here: a region straddling the 3x3 boundary must still run the draw block for its inner
        // half — the per-slot fold enforces the cap exactly.
        int foldedOriginX = XaeroWorldMapFold.foldBlock(Direction.Axis.X, regX * 512);
        int foldedOriginZ = XaeroWorldMapFold.foldBlock(Direction.Axis.Z, regZ * 512);
        return processor.getLeafMapRegion(
                caveLayer, Math.floorDiv(foldedOriginX, 512), Math.floorDiv(foldedOriginZ, 512), false);
    }

    @Redirect(
            method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"},
            at = @At(value = "INVOKE", target = "Lxaero/map/region/LeveledRegion;hasTextures()Z"))
    private boolean toroidal$candidateHasTextures(LeveledRegion<?> region) {
        if (XaeroWorldMapFold.active() && region != null && region == this.toroidal$leveledCandidate) {
            return true;
        }

        return region.hasTextures();
    }

    @Redirect(
            method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"},
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
            method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"},
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

    // At a zoom whose texture slot the world does not align to (the deepest zoom-out), the slot substitution is
    // off — every slot straddles the world edge internally, and its texture's empty half is opaque. There the
    // copies come clipped: the original full-rect draw is replaced by a sub-rect draw of just the slot's
    // world-overlapping part, repeated at the eight period offsets — no empty texel is ever drawn, so the draw
    // order stops mattering. The two glue mechanisms are gated on the same alignment test, so exactly one runs at
    // any zoom. The slot's view position rides in from the texture-fetch redirect that always precedes this draw.
    @WrapOperation(
            method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"},
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/gui/GuiMap;renderTexturedModalRectWithLighting3(Lorg/joml/Matrix4f;FFFFIZLxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRenderer;)V"))
    private void toroidal$drawClippedPeriodCopies(
            Matrix4f matrix, float x, float y, float width, float height,
            int texture, boolean hasLight, MultiTextureRenderTypeRenderer renderer, Operation<Void> original) {
        int slotSize = 64 << this.toroidal$viewLevel;
        int[] xBounds;
        int[] zBounds;
        if (!XaeroWorldMapFold.active()
                || XaeroWorldMapFold.glueableAt(slotSize)
                || (xBounds = XaeroWorldMapFold.seamBounds(Direction.Axis.X)) == null
                || (zBounds = XaeroWorldMapFold.seamBounds(Direction.Axis.Z)) == null) {
            original.call(matrix, x, y, width, height, texture, hasLight, renderer);
            return;
        }

        int slotMinX = this.toroidal$slotViewBlockX;
        int slotMinZ = this.toroidal$slotViewBlockZ;
        int clippedMinX = Math.max(slotMinX, xBounds[0]);
        int clippedMaxX = Math.min(slotMinX + slotSize, xBounds[0] + xBounds[1]);
        int clippedMinZ = Math.max(slotMinZ, zBounds[0]);
        int clippedMaxZ = Math.min(slotMinZ + slotSize, zBounds[0] + zBounds[1]);
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
        // The quad is emitted directly (the same four vertices GuiMap's own sub-rect helper writes) — calling the
        // static helper would drag GuiMap's xaerolib superclass onto the compile classpath.
        for (int periodX = -1; periodX <= 1; periodX++) {
            for (int periodZ = -1; periodZ <= 1; periodZ++) {
                float copyX = clippedX + periodX * xBounds[1];
                float copyY = clippedY + periodZ * zBounds[1];
                BufferBuilder quad = renderer.begin(texture);
                quad.addVertex(matrix, copyX, copyY + clippedHeight, 0.0F).setUv(u1, v2);
                quad.addVertex(matrix, copyX + clippedWidth, copyY + clippedHeight, 0.0F).setUv(u2, v2);
                quad.addVertex(matrix, copyX + clippedWidth, copyY, 0.0F).setUv(u2, v1);
                quad.addVertex(matrix, copyX, copyY, 0.0F).setUv(u1, v1);
            }
        }
    }

    // The white seam grid — the JourneyMap-style outline of the canonical world and its eight glued copies. Drawn
    // by wrapping the cursor-chunk highlight: it renders every frame in exactly the map-space pass and buffer the
    // lines need, so its arguments carry the whole transform. The thickness tracks one screen pixel, floored at
    // one block.
    @WrapOperation(
            method = {"render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", "method_25394(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"},
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

        int[] xBounds = XaeroWorldMapFold.seamBounds(Direction.Axis.X);
        int[] zBounds = XaeroWorldMapFold.seamBounds(Direction.Axis.Z);
        if (xBounds == null || zBounds == null) {
            return;
        }

        int thickness = Math.max(1, (int) Math.ceil(1.0 / this.scale));
        int gridMinX = xBounds[0] - xBounds[1];
        int gridMaxX = xBounds[0] + 2 * xBounds[1];
        int gridMinZ = zBounds[0] - zBounds[1];
        int gridMaxZ = zBounds[0] + 2 * zBounds[1];
        Matrix4f matrix = matrixStack.last().pose();
        for (int lineIndex = 0; lineIndex <= 3; lineIndex++) {
            int lineX = gridMinX + lineIndex * xBounds[1];
            MapRenderHelper.fillIntoExistingBuffer(matrix, overlayBuffer,
                    lineX - flooredCameraX, gridMinZ - flooredCameraZ,
                    lineX - flooredCameraX + thickness, gridMaxZ - flooredCameraZ,
                    1.0F, 1.0F, 1.0F, 0.8F);
            int lineZ = gridMinZ + lineIndex * zBounds[1];
            MapRenderHelper.fillIntoExistingBuffer(matrix, overlayBuffer,
                    gridMinX - flooredCameraX, lineZ - flooredCameraZ,
                    gridMaxX - flooredCameraX, lineZ - flooredCameraZ + thickness,
                    1.0F, 1.0F, 1.0F, 0.8F);
        }
    }
}
