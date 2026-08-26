package com.toroidalworld.compat.c2me;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.noise.ContextScaledNoise;
import com.toroidalworld.noise.SlotAxes;
import com.ishland.c2me.opts.dfc.common.gen.jvm.BytecodeEmitter;
import com.ishland.c2me.opts.dfc.common.gen.jvm.BytecodeGen;
import com.ishland.c2me.opts.dfc.common.gen.jvm.util.DfcObjectCache;
import com.ishland.c2me.opts.dfc.common.gen.meta.ValuesMethodDefF64;

import net.minecraft.world.level.levelgen.DensityFunction.NoiseHolder;

public final class C2meFoldedNoiseEmitter implements BytecodeEmitter<C2meFoldedNoiseNode> {
    public static final C2meFoldedNoiseEmitter INSTANCE = new C2meFoldedNoiseEmitter();

    private static final String SAMPLE_CLASS = Type.getInternalName(ContextScaledNoise.class);
    private static final String TRANSFORMER_DESC = Type.getDescriptor(WorldFold.class);
    private static final String SLOT_AXES_DESC = Type.getDescriptor(SlotAxes.class);
    private static final String NOISE_HOLDER_DESC = Type.getDescriptor(NoiseHolder.class);

    private static final String SAMPLE_METHOD = "sampleWrapped";
    private static final String SAMPLE_DESC = Type.getMethodDescriptor(
            Type.DOUBLE_TYPE,
            Type.getType(WorldFold.class),
            Type.getType(SlotAxes.class),
            Type.getType(NoiseHolder.class),
            Type.DOUBLE_TYPE,
            Type.DOUBLE_TYPE,
            Type.DOUBLE_TYPE,
            Type.DOUBLE_TYPE);

    private static final String LOOP_CLASS = Type.getInternalName(C2meFoldedNoiseLoop.class);

    private static final String FILL_METHOD = "fill";
    private static final String FILL_DESC = Type.getMethodDescriptor(
            Type.VOID_TYPE,
            Type.getType(WorldFold.class),
            Type.getType(SlotAxes.class),
            Type.getType(NoiseHolder.class),
            Type.getType(double[].class),
            Type.getType(double[].class),
            Type.DOUBLE_TYPE,
            Type.getType(double[].class),
            Type.DOUBLE_TYPE,
            Type.getType(double[].class),
            Type.DOUBLE_TYPE,
            Type.DOUBLE_TYPE);

    // C2ME's own arrangement: the result array doubles as the buffer for the first input that needs one.
    private static final int RESULT_ARRAY_LOCAL = 1;

    private static final int OBJECT_CACHE_LOCAL = 6;

    private C2meFoldedNoiseEmitter() {
    }

    @Override
    public void doBytecodeGenSingle(C2meFoldedNoiseNode node, BytecodeGen.Context context, InstructionAdapter m,
            BytecodeGen.Context.LocalVarConsumer localVarConsumer) {
        String noiseField = context.newField(NoiseHolder.class, node.noise);
        String transformerField = context.newField(WorldFold.class, node.transformer);
        String slotAxesField = context.newField(SlotAxes.class, node.slotAxes);
        ValuesMethodDefF64 foldedXMethod = context.newSingleMethodF64(node.foldedX);
        ValuesMethodDefF64 foldedYMethod = context.newSingleMethodF64(node.foldedY);
        ValuesMethodDefF64 foldedZMethod = context.newSingleMethodF64(node.foldedZ);

        m.load(0, InstructionAdapter.OBJECT_TYPE);
        m.getfield(context.className, transformerField, TRANSFORMER_DESC);
        m.load(0, InstructionAdapter.OBJECT_TYPE);
        m.getfield(context.className, slotAxesField, SLOT_AXES_DESC);
        m.load(0, InstructionAdapter.OBJECT_TYPE);
        m.getfield(context.className, noiseField, NOISE_HOLDER_DESC);
        context.callDelegateSingle(m, foldedXMethod);
        context.callDelegateSingle(m, foldedYMethod);
        context.callDelegateSingle(m, foldedZMethod);
        m.dconst(node.horizontalScale);
        m.invokestatic(SAMPLE_CLASS, SAMPLE_METHOD, SAMPLE_DESC, false);
        m.areturn(Type.DOUBLE_TYPE);
    }

    @Override
    public void doBytecodeGenMulti(C2meFoldedNoiseNode node, BytecodeGen.Context context, InstructionAdapter m,
            BytecodeGen.Context.LocalVarConsumer localVarConsumer) {
        String noiseField = context.newField(NoiseHolder.class, node.noise);
        String transformerField = context.newField(WorldFold.class, node.transformer);
        String slotAxesField = context.newField(SlotAxes.class, node.slotAxes);

        ValuesMethodDefF64 foldedXMethod = context.newMultiMethodF64(node.foldedX);
        ValuesMethodDefF64 foldedYMethod = context.newMultiMethodF64(node.foldedY);
        ValuesMethodDefF64 foldedZMethod = context.newMultiMethodF64(node.foldedZ);
        boolean constantX = foldedXMethod.isConst();
        boolean constantY = foldedYMethod.isConst();
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
            context.callDelegateMulti(m, foldedYMethod, arrays[filledArrays++]);
        }

        if (!constantZ) {
            context.callDelegateMulti(m, foldedZMethod, arrays[filledArrays++]);
        }

        m.load(0, InstructionAdapter.OBJECT_TYPE);
        m.getfield(context.className, transformerField, TRANSFORMER_DESC);
        m.load(0, InstructionAdapter.OBJECT_TYPE);
        m.getfield(context.className, slotAxesField, SLOT_AXES_DESC);
        m.load(0, InstructionAdapter.OBJECT_TYPE);
        m.getfield(context.className, noiseField, NOISE_HOLDER_DESC);
        m.load(RESULT_ARRAY_LOCAL, InstructionAdapter.OBJECT_TYPE);

        int readArrays = 0;
        readArrays = loadAxis(m, arrays, readArrays, foldedXMethod, constantX);
        readArrays = loadAxis(m, arrays, readArrays, foldedYMethod, constantY);
        loadAxis(m, arrays, readArrays, foldedZMethod, constantZ);

        m.dconst(node.horizontalScale);
        m.invokestatic(LOOP_CLASS, FILL_METHOD, FILL_DESC, false);

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

    private static int loadAxis(InstructionAdapter m, int[] arrays, int readArrays,
            ValuesMethodDefF64 method, boolean constant) {
        if (constant) {
            m.aconst(null);
            m.dconst(method.constValue());
            return readArrays;
        }

        m.load(arrays[readArrays], InstructionAdapter.OBJECT_TYPE);
        m.dconst(0.0);
        return readArrays + 1;
    }
}
