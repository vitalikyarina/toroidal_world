package com.toroidalworld.compat.ftbchunks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.toroidalworld.compat.ftbchunks.FtbChunksFold;

import net.minecraft.world.phys.Vec3;

import dev.ftb.mods.ftbchunks.client.gui.MapIconWidget;

@Mixin(value = MapIconWidget.class, remap = false)
public abstract class MapIconWidgetMixin {
    @ModifyExpressionValue(method = "updatePosition",
            at = @At(value = "INVOKE",
                    target = "Ldev/ftb/mods/ftbchunks/api/client/icon/MapIcon;getPos(F)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$foldIconPosition(Vec3 position) {
        return FtbChunksFold.foldPosition(position);
    }
}
