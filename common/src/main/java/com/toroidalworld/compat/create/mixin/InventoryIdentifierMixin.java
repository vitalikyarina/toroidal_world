package com.toroidalworld.compat.create.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.api.packager.InventoryIdentifier;
import com.toroidalworld.compat.create.CreateInventoryFold;

import net.minecraft.world.level.Level;

@Mixin(value = InventoryIdentifier.class, remap = false)
public interface InventoryIdentifierMixin {
    @ModifyReturnValue(method = "get", at = @At("RETURN"))
    private static @Nullable InventoryIdentifier toroidal$foldIntoTheCanonicalFrame(
            @Nullable InventoryIdentifier identifier, @Local(argsOnly = true) Level level) {
        return CreateInventoryFold.fold(level, identifier);
    }
}
