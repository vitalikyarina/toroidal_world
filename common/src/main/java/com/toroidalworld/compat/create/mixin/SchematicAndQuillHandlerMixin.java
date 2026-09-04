package com.toroidalworld.compat.create.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.schematics.client.SchematicAndQuillHandler;
import com.toroidalworld.compat.create.client.CreateSchematicSelection;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

@Mixin(value = SchematicAndQuillHandler.class, remap = false)
public class SchematicAndQuillHandlerMixin {
    @Shadow
    public BlockPos firstPos;

    @Shadow
    private BlockPos selectedPos;

    @Inject(method = "onMouseInput", cancellable = true,
            at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
                    target = "Lcom/simibubi/create/content/schematics/client/SchematicAndQuillHandler;"
                            + "secondPos:Lnet/minecraft/core/BlockPos;"))
    private void toroidal$refuseOversizedSelection(int button, boolean pressed,
            CallbackInfoReturnable<Boolean> callback) {
        if (CreateSchematicSelection.refuseOversizedCorner(firstPos, selectedPos)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", cancellable = true,
            at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
                    target = "Lcom/simibubi/create/content/schematics/client/SchematicAndQuillHandler;"
                            + "firstPos:Lnet/minecraft/core/BlockPos;"))
    private void toroidal$refuseOversizedGrowth(double delta, CallbackInfoReturnable<Boolean> callback,
            @Local(name = "bb") AABB grown) {
        if (CreateSchematicSelection.refuseOversizedGrowth(
                BlockPos.containing(grown.minX, grown.minY, grown.minZ),
                BlockPos.containing(grown.maxX, grown.maxY, grown.maxZ))) {
            callback.setReturnValue(true);
        }
    }
}
