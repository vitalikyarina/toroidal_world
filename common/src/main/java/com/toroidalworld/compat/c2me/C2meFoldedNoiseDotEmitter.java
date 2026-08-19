package com.toroidalworld.compat.c2me;

import com.ishland.c2me.opts.dfc.common.gen.dot.DotEmitter;
import com.ishland.c2me.opts.dfc.common.gen.dot.DotGen;

// C2ME draws every function it compiles and its registry throws on an unknown node, so a folded node with no drawing fails world creation.
public final class C2meFoldedNoiseDotEmitter implements DotEmitter<C2meFoldedNoiseNode> {
    public static final C2meFoldedNoiseDotEmitter INSTANCE = new C2meFoldedNoiseDotEmitter();

    private C2meFoldedNoiseDotEmitter() {
    }

    @Override
    public int doDotGen(C2meFoldedNoiseNode node, DotGen.Context context, DotGen.Context.Builder builder) {
        return builder.hexagonShape()
                .label("ToroidalFoldedNoise\\nscale=" + node.horizontalScale)
                .edge(context.generate(node.foldedX))
                .label("foldedX")
                .finish()
                .edge(context.generate(node.inputY))
                .label("inputY")
                .finish()
                .edge(context.generate(node.foldedZ))
                .label("foldedZ")
                .finish()
                .edge(context.generate(node.inputX))
                .label("inputX")
                .finish()
                .edge(context.generate(node.inputZ))
                .label("inputZ")
                .finish()
                .build();
    }
}
