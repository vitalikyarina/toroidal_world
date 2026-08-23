package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.schematics.SchematicPrinter;
import com.simibubi.create.content.schematics.packet.SchematicPlacePacket;
import com.toroidalworld.compat.create.CreateSchematicFold;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@Mixin(value = SchematicPlacePacket.class, remap = false)
public class SchematicPlacePacketMixin {
    @WrapOperation(
            method = "handle",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/schematics/SchematicPrinter;"
                            + "loadSchematic(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Z)V"))
    private void toroidal$anchorInstantPrintOnPlayer(SchematicPrinter printer, ItemStack blueprint, Level level,
            boolean processNBT, Operation<Void> original, @Local(argsOnly = true) ServerPlayer player) {
        original.call(printer, CreateSchematicFold.anchoredNear(level, player.blockPosition(), blueprint), level,
                processNBT);
    }
}
