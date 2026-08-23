package com.toroidalworld.compat.create.mixin;

import java.nio.file.Path;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.content.schematics.SchematicExport;
import com.toroidalworld.compat.create.CreateSchematicFold;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

@Mixin(value = SchematicExport.class, remap = false)
public class SchematicExportMixin {
    @Inject(method = "saveSchematic", at = @At("HEAD"), cancellable = true)
    private static void toroidal$refuseRegionWiderThanWorld(Path dir, String fileName, boolean overwrite, Level level,
            BlockPos first, BlockPos second, CallbackInfoReturnable<SchematicExport.SchematicExportResult> callback) {
        if (CreateSchematicFold.regionExceedsWorld(level, first, second)) {
            callback.setReturnValue(null);
        }
    }
}
