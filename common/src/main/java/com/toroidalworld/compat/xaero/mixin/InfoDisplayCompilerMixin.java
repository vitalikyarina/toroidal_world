package com.toroidalworld.compat.xaero.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.compat.xaero.XaeroFold;

import net.minecraft.core.BlockPos;

// One compile call per info display per frame, and the player position rides in as an argument — the single point
// where the coordinate readouts under the minimap can be folded per display, leaving the level-querying displays
// (biome, light) their mirror position. Which displays get the fold is decided in XaeroFold by identity against
// the BuiltInInfoDisplays constants.
@Mixin(targets = "xaero.hud.minimap.info.render.compile.InfoDisplayCompiler", remap = false)
public abstract class InfoDisplayCompilerMixin {
    @ModifyVariable(method = "compile", at = @At("HEAD"), argsOnly = true)
    private BlockPos toroidal$foldCoordReadouts(BlockPos playerPos,
            @Coerce Object infoDisplay, @Coerce Object session, int size, BlockPos playerPosArg) {
        return XaeroFold.foldInfoDisplayPos(infoDisplay, playerPos);
    }
}
