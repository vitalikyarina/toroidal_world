package com.toroidalworld.compat.c2me.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.toroidalworld.compat.c2me.C2meDfcAst;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.McToAst;

import net.minecraft.world.level.levelgen.DensityFunction;

// The one door every density function walks through on its way into C2ME's compiler, and the last place a noise node
// is still an object rather than bytecode.
//
// Wrapping the translation rather than registering a competing one: C2ME's frontend registry rejects a second entry
// for a class it already knows and freezes itself at first use, so the only way to say something else about Noise is
// to let it speak first.
@Mixin(McToAst.class)
public class McToAstMixin {
    @WrapMethod(method = "toAst(Lnet/minecraft/world/level/levelgen/DensityFunction;)"
            + "Lcom/ishland/c2me/opts/dfc/common/ast/AstNode;")
    private static AstNode toroidal$foldNoiseNodes(DensityFunction densityFunction, Operation<AstNode> original) {
        return C2meDfcAst.fold(densityFunction, original.call(densityFunction));
    }
}
