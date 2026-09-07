package com.toroidalworld.compat.create.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.trains.entity.CarriageBogey;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntityRenderer;
import com.toroidalworld.compat.create.client.CreateClientFrame;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = CarriageContraptionEntityRenderer.class, remap = false)
public abstract class CarriageContraptionEntityRendererMixin {
    @ModifyExpressionValue(method = "getBogeyLightCoords",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/trains/entity/CarriageBogey;"
                            + "getAnchorPosition()Lnet/minecraft/world/phys/Vec3;"))
    private static @Nullable Vec3 toroidal$anchorInEntityFrame(@Nullable Vec3 anchor,
            CarriageContraptionEntity entity, CarriageBogey bogey, float partialTicks) {
        if (anchor == null) {
            return null;
        }

        return CreateClientFrame.nearestCopy(((Entity) (Object) entity).position(), anchor);
    }
}
