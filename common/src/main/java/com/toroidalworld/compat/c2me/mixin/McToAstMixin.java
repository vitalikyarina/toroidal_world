package com.toroidalworld.compat.c2me.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.toroidalworld.compat.c2me.C2meDfcAst;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.McToAst;

import net.minecraft.world.level.levelgen.DensityFunction;

@Mixin(McToAst.class)
public class McToAstMixin {
    @WrapMethod(method = "toAst(Lnet/minecraft/world/level/levelgen/DensityFunction;)"
            + "Lcom/ishland/c2me/opts/dfc/common/ast/AstNode;")
    private static AstNode toroidal$foldNoiseNodes(DensityFunction densityFunction, Operation<AstNode> original) {
        return C2meDfcAst.fold(densityFunction, original.call(densityFunction));
    }
}
