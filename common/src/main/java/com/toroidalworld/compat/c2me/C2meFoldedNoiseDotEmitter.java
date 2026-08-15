package com.toroidalworld.compat.c2me;

import com.ishland.c2me.opts.dfc.common.gen.dot.DotEmitter;
import com.ishland.c2me.opts.dfc.common.gen.dot.DotGen;

// Not a debugging nicety: C2ME writes a graph of every function it compiles at the end of each compilation, without
// asking, and its emitter registry throws on a node class it does not know. A folded node with no drawing is a world
// that fails to create.
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
