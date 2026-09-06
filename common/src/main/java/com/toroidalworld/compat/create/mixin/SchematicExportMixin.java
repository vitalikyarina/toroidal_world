package com.toroidalworld.compat.create.mixin;

import java.nio.file.Path;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.schematics.SchematicExport;
import com.toroidalworld.compat.create.CreateSchematicFold;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

@Mixin(value = SchematicExport.class, remap = false)
public class SchematicExportMixin {
    @WrapMethod(method = "saveSchematic")
    private static SchematicExport.SchematicExportResult toroidal$refuseRegionWiderThanWorld(Path dir, String fileName,
            boolean overwrite, Level level, BlockPos first, BlockPos second,
            Operation<SchematicExport.SchematicExportResult> original) {
        if (CreateSchematicFold.regionExceedsWorld(level, first, second)) {
            return null;
        }

        return original.call(dir, fileName, overwrite, level, first, second);
    }
}
