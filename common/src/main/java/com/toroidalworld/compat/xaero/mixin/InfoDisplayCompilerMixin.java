package com.toroidalworld.compat.xaero.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.compat.xaero.XaeroFold;

import net.minecraft.core.BlockPos;

@Mixin(targets = "xaero.hud.minimap.info.render.compile.InfoDisplayCompiler", remap = false)
public abstract class InfoDisplayCompilerMixin {
    @ModifyVariable(method = "compile", at = @At("HEAD"), argsOnly = true)
    private BlockPos toroidal$foldCoordReadouts(BlockPos playerPos,
            @Coerce Object infoDisplay, @Coerce Object session, int size, BlockPos playerPosArg) {
        return XaeroFold.foldInfoDisplayPos(infoDisplay, playerPos);
    }
}
