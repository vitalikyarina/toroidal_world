package com.toroidalworld.compat.c2me;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.noise.ContextScaledNoise;
import com.ishland.c2me.opts.dfc.common.gen.jvm.BytecodeEmitter;
import com.ishland.c2me.opts.dfc.common.gen.jvm.BytecodeGen;
import com.ishland.c2me.opts.dfc.common.gen.jvm.util.DfcObjectCache;
import com.ishland.c2me.opts.dfc.common.gen.meta.ValuesMethodDefF64;

import net.minecraft.world.level.levelgen.DensityFunction.NoiseHolder;

public final class C2meFoldedNoiseEmitter implements BytecodeEmitter<C2meFoldedNoiseNode> {
    public static final C2meFoldedNoiseEmitter INSTANCE = new C2meFoldedNoiseEmitter();

    private static final String SAMPLE_CLASS = Type.getInternalName(ContextScaledNoise.class);
    private static final String TRANSFORMER_DESC = Type.getDescriptor(WorldLoopTransformer.class);
    private static final String NOISE_HOLDER_DESC = Type.getDescriptor(NoiseHolder.class);

    private static final String SAMPLE_METHOD = "sampleWrapped";
    private static final String SAMPLE_DESC = Type.getMethodDescriptor(
            Type.DOUBLE_TYPE,
            Type.getType(WorldLoopTransformer.class),
            Type.getType(NoiseHolder.class),
            Type.DOUBLE_TYPE,
            Type.DOUBLE_TYPE,
            Type.DOUBLE_TYPE,
            Type.DOUBLE_TYPE);

    private static final String TRANSFORMER_LOCAL = "toroidalTransformer";

    // C2ME's own arrangement: the result array doubles as the buffer for the first input that needs one.
    private static final int RESULT_ARRAY_LOCAL = 1;

    private static final int OBJECT_CACHE_LOCAL = 6;

    private C2meFoldedNoiseEmitter() {
    }

    @Override
    public void doBytecodeGenSingle(C2meFoldedNoiseNode node, BytecodeGen.Context context, InstructionAdapter m,
            BytecodeGen.Context.LocalVarConsumer localVarConsumer) {
        String noiseField = context.newField(NoiseHolder.class, node.noise);
        String transformerField = context.newField(WorldLoopTransformer.class, node.transformer);
        ValuesMethodDefF64 foldedXMethod = context.newSingleMethodF64(node.foldedX);
        ValuesMethodDefF64 inputYMethod = context.newSingleMethodF64(node.inputY);
        ValuesMethodDefF64 foldedZMethod = context.newSingleMethodF64(node.foldedZ);

        m.load(0, InstructionAdapter.OBJECT_TYPE);
        m.getfield(context.className, transformerField, TRANSFORMER_DESC);
        m.load(0, InstructionAdapter.OBJECT_TYPE);
        m.getfield(context.className, noiseField, NOISE_HOLDER_DESC);
        context.callDelegateSingle(m, foldedXMethod);
        context.callDelegateSingle(m, inputYMethod);
        context.callDelegateSingle(m, foldedZMethod);
        m.dconst(node.horizontalScale);
        m.invokestatic(SAMPLE_CLASS, SAMPLE_METHOD, SAMPLE_DESC, false);
        m.areturn(Type.DOUBLE_TYPE);
    }

    @Override
    public void doBytecodeGenMulti(C2meFoldedNoiseNode node, BytecodeGen.Context context, InstructionAdapter m,
            BytecodeGen.Context.LocalVarConsumer localVarConsumer) {
        String noiseField = context.newField(NoiseHolder.class, node.noise);
        String transformerField = context.newField(WorldLoopTransformer.class, node.transformer);

        // Hoisted out of the counted loop: one field read per array fill rather than one per element.
        int transformerLocal = localVarConsumer.createLocalVariable(TRANSFORMER_LOCAL, TRANSFORMER_DESC);
        m.load(0, InstructionAdapter.OBJECT_TYPE);
        m.getfield(context.className, transformerField, TRANSFORMER_DESC);
        m.store(transformerLocal, InstructionAdapter.OBJECT_TYPE);

        ValuesMethodDefF64 foldedXMethod = context.newMultiMethodF64(node.foldedX);
        ValuesMethodDefF64 inputYMethod = context.newMultiMethodF64(node.inputY);
        ValuesMethodDefF64 foldedZMethod = context.newMultiMethodF64(node.foldedZ);
        boolean constantX = foldedXMethod.isConst();
        boolean constantY = inputYMethod.isConst();
        boolean constantZ = foldedZMethod.isConst();

        int arraysNeeded = (constantX ? 0 : 1) + (constantY ? 0 : 1) + (constantZ ? 0 : 1);
        int[] arrays = new int[arraysNeeded];
        if (arraysNeeded >= 1) {
            arrays[0] = RESULT_ARRAY_LOCAL;
        }

        for (int arrayIdx = 1; arrayIdx < arraysNeeded; arrayIdx++) {
            arrays[arrayIdx] = localVarConsumer.createLocalVariable("foldedRes" + arrayIdx,
                    Type.getDescriptor(double[].class));
            m.load(OBJECT_CACHE_LOCAL, InstructionAdapter.OBJECT_TYPE);
            m.load(RESULT_ARRAY_LOCAL, InstructionAdapter.OBJECT_TYPE);
            m.arraylength();
            m.iconst(0);
            m.invokeinterface(
                    Type.getInternalName(DfcObjectCache.class),
                    "getDoubleArray",
                    Type.getMethodDescriptor(Type.getType(double[].class), Type.INT_TYPE, Type.BOOLEAN_TYPE));
            m.store(arrays[arrayIdx], InstructionAdapter.OBJECT_TYPE);
        }

        int filledArrays = 0;
        if (!constantX) {
            context.callDelegateMulti(m, foldedXMethod, arrays[filledArrays++]);
        }

        if (!constantY) {
            context.callDelegateMulti(m, inputYMethod, arrays[filledArrays++]);
        }

        if (!constantZ) {
            context.callDelegateMulti(m, foldedZMethod, arrays[filledArrays++]);
        }

        context.doCountedLoop(m, localVarConsumer, idx -> {
            m.load(RESULT_ARRAY_LOCAL, InstructionAdapter.OBJECT_TYPE);
            m.load(idx, Type.INT_TYPE);
            m.load(transformerLocal, InstructionAdapter.OBJECT_TYPE);
            m.load(0, InstructionAdapter.OBJECT_TYPE);
            m.getfield(context.className, noiseField, NOISE_HOLDER_DESC);

            int readArrays = 0;
            readArrays = loadInput(m, idx, arrays, readArrays, foldedXMethod, constantX);
            readArrays = loadInput(m, idx, arrays, readArrays, inputYMethod, constantY);
            loadInput(m, idx, arrays, readArrays, foldedZMethod, constantZ);

            m.dconst(node.horizontalScale);
            m.invokestatic(SAMPLE_CLASS, SAMPLE_METHOD, SAMPLE_DESC, false);
            m.astore(Type.DOUBLE_TYPE);
        });

        for (int arrayIdx = 1; arrayIdx < arrays.length; arrayIdx++) {
            m.load(OBJECT_CACHE_LOCAL, InstructionAdapter.OBJECT_TYPE);
            m.load(arrays[arrayIdx], InstructionAdapter.OBJECT_TYPE);
            m.invokeinterface(
                    Type.getInternalName(DfcObjectCache.class),
                    "recycle",
                    Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(double[].class)));
        }

        m.areturn(Type.VOID_TYPE);
    }

    private static int loadInput(InstructionAdapter m, int idx, int[] arrays, int readArrays,
            ValuesMethodDefF64 method, boolean constant) {
        if (constant) {
            m.dconst(method.constValue());
            return readArrays;
        }

        m.load(arrays[readArrays], InstructionAdapter.OBJECT_TYPE);
        m.load(idx, Type.INT_TYPE);
        m.aload(Type.DOUBLE_TYPE);
        return readArrays + 1;
    }
}
