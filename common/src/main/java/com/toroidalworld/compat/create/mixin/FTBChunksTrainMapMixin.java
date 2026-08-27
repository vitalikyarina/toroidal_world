package com.toroidalworld.compat.create.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.compat.trainmap.FTBChunksTrainMap;
import com.toroidalworld.compat.create.client.TrainMapSurface;
import com.toroidalworld.map.MapSurfaceCopies.Copies;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.FormattedText;

@Mixin(value = FTBChunksTrainMap.class, remap = false)
public abstract class FTBChunksTrainMapMixin {
    @WrapOperation(method = "renderGui",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/compat/trainmap/TrainMapManager;renderAndPick"
                            + "(Lnet/minecraft/client/gui/GuiGraphics;IIZLnet/minecraft/client/renderer/Rect2i;)"
                            + "Ljava/util/List;"))
    private static List<FormattedText> toroidal$onAnUntiledSurface(GuiGraphics graphics, int mouseX, int mouseY,
            boolean linearFiltering, Rect2i bounds, Operation<List<FormattedText>> original) {
        return TrainMapSurface.showing(Copies.NONE,
                () -> original.call(graphics, mouseX, mouseY, linearFiltering, bounds));
    }
}
