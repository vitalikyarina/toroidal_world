package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.schematics.SchematicPrinter;
import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;
import com.toroidalworld.compat.create.CreateSchematicFold;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

@Mixin(value = SchematicannonBlockEntity.class, remap = false)
public class SchematicannonPrinterMixin {
    @WrapOperation(
            method = "initializePrinter",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/schematics/SchematicPrinter;"
                            + "loadSchematic(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Z)V"))
    private void toroidal$anchorPrinterOnCannon(SchematicPrinter printer, ItemStack blueprint, Level level,
            boolean processNBT, Operation<Void> original) {
        BlockEntity cannon = (BlockEntity) (Object) this;
        original.call(printer, CreateSchematicFold.anchoredNear(level, cannon.getBlockPos(), blueprint), level,
                processNBT);
    }
}
