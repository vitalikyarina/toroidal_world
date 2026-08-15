package com.toroidalworld.compat.c2me;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import com.toroidalworld.noise.GenerationTransformerContext.Context;
import com.ishland.c2me.opts.dfc.common.gen.jvm.BytecodeEmitter;
import com.ishland.c2me.opts.dfc.common.gen.jvm.BytecodeGen;
import com.ishland.c2me.opts.dfc.common.gen.jvm.emitters.misc.GenericShiftedNoiseNodeBytecodeEmitter;
import com.ishland.c2me.opts.dfc.common.gen.jvm.util.DfcObjectCache;
import com.ishland.c2me.opts.dfc.common.gen.meta.ValuesMethodDefF64;

import net.minecraft.world.level.levelgen.DensityFunction.NoiseHolder;

// Writes the branch C2ME's own emitter has no reason to write: ask once whether this generation wraps, and evaluate
// the folded inputs or C2ME's own accordingly. The vanilla side is not a copy — it is C2ME's emitter, handed this node
// directly, because the node's inherited inputs are C2ME's tree untouched.
//
// Both branches end in their own return rather than meeting at a merge point, which is what lets the vanilla side be
// delegated whole: its emitter ends every method it writes.
public final class C2meFoldedNoiseEmitter implements BytecodeEmitter<C2meFoldedNoiseNode> {
    public static final C2meFoldedNoiseEmitter INSTANCE = new C2meFoldedNoiseEmitter();

    private static final String FOLD_CLASS = Type.getInternalName(C2meDfcFold.class);
    private static final String CONTEXT_DESC = Type.getDescriptor(Context.class);
    private static final String NOISE_HOLDER_DESC = Type.getDescriptor(NoiseHolder.class);

    private static final String WRAPPED_CONTEXT_METHOD = "wrappedContext";
    private static final String WRAPPED_CONTEXT_DESC = Type.getMethodDescriptor(Type.getType(Context.class));

    private static final String SAMPLE_METHOD = "sample";
    private static final String SAMPLE_DESC = Type.getMethodDescriptor(
            Type.DOUBLE_TYPE,
            Type.getType(Context.class),
            Type.getType(NoiseHolder.class),
            Type.DOUBLE_TYPE,
            Type.DOUBLE_TYPE,
            Type.DOUBLE_TYPE,
            Type.DOUBLE_TYPE);

    private static final String GENERATION_LOCAL = "toroidalGeneration";

    // The result array of a multi method doubles as the buffer for the first input that needs one — C2ME's own
    // arrangement, kept here because the loop reads every input before it writes the result back over the first.
    private static final int RESULT_ARRAY_LOCAL = 1;

    private static final int OBJECT_CACHE_LOCAL = 6;

    private C2meFoldedNoiseEmitter() {
    }

    @Override
    public void doBytecodeGenSingle(C2meFoldedNoiseNode node, BytecodeGen.Context context, InstructionAdapter m,
            BytecodeGen.Context.LocalVarConsumer localVarConsumer) {
        String noiseField = context.newField(NoiseHolder.class, node.noise);
        ValuesMethodDefF64 foldedXMethod = context.newSingleMethodF64(node.foldedX);
        ValuesMethodDefF64 inputYMethod = context.newSingleMethodF64(node.inputY);
        ValuesMethodDefF64 foldedZMethod = context.newSingleMethodF64(node.foldedZ);

        Label vanilla = new Label();
        int generationLocal = emitWrappedContext(m, localVarConsumer, vanilla);

        m.load(generationLocal, InstructionAdapter.OBJECT_TYPE);
        m.load(0, InstructionAdapter.OBJECT_TYPE);
        m.getfield(context.className, noiseField, NOISE_HOLDER_DESC);
        context.callDelegateSingle(m, foldedXMethod);
        context.callDelegateSingle(m, inputYMethod);
        context.callDelegateSingle(m, foldedZMethod);
        m.dconst(node.horizontalScale);
        m.invokestatic(FOLD_CLASS, SAMPLE_METHOD, SAMPLE_DESC, false);
        m.areturn(Type.DOUBLE_TYPE);

        m.visitLabel(vanilla);
        GenericShiftedNoiseNodeBytecodeEmitter.INSTANCE.doBytecodeGenSingle(node, context, m, localVarConsumer);
    }

    @Override
    public void doBytecodeGenMulti(C2meFoldedNoiseNode node, BytecodeGen.Context context, InstructionAdapter m,
            BytecodeGen.Context.LocalVarConsumer localVarConsumer) {
        Label vanilla = new Label();
        int generationLocal = emitWrappedContext(m, localVarConsumer, vanilla);
        emitFoldedMulti(node, context, m, localVarConsumer, generationLocal);

        m.visitLabel(vanilla);
        GenericShiftedNoiseNodeBytecodeEmitter.INSTANCE.doBytecodeGenMulti(node, context, m, localVarConsumer);
    }

    // The one lookup, kept in a local so the sample can be handed the same context the branch was decided on.
    private static int emitWrappedContext(InstructionAdapter m, BytecodeGen.Context.LocalVarConsumer localVarConsumer,
            Label vanilla) {
        int generationLocal = localVarConsumer.createLocalVariable(GENERATION_LOCAL, CONTEXT_DESC);
        m.invokestatic(FOLD_CLASS, WRAPPED_CONTEXT_METHOD, WRAPPED_CONTEXT_DESC, false);
        m.store(generationLocal, InstructionAdapter.OBJECT_TYPE);
        m.load(generationLocal, InstructionAdapter.OBJECT_TYPE);
        m.ifnull(vanilla);
        return generationLocal;
    }

    // C2ME's array plumbing for a three-input noise node, with the folded inputs in place of the scaled ones and the
    // scale riding into the call. An input that folded to a constant carries no array at all, which is the case for
    // every ShiftB — its third input is the constant zero.
    private static void emitFoldedMulti(C2meFoldedNoiseNode node, BytecodeGen.Context context, InstructionAdapter m,
            BytecodeGen.Context.LocalVarConsumer localVarConsumer, int generationLocal) {
        String noiseField = context.newField(NoiseHolder.class, node.noise);
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
            m.load(generationLocal, InstructionAdapter.OBJECT_TYPE);
            m.load(0, InstructionAdapter.OBJECT_TYPE);
            m.getfield(context.className, noiseField, NOISE_HOLDER_DESC);

            int readArrays = 0;
            readArrays = loadInput(m, idx, arrays, readArrays, foldedXMethod, constantX);
            readArrays = loadInput(m, idx, arrays, readArrays, inputYMethod, constantY);
            loadInput(m, idx, arrays, readArrays, foldedZMethod, constantZ);

            m.dconst(node.horizontalScale);
            m.invokestatic(FOLD_CLASS, SAMPLE_METHOD, SAMPLE_DESC, false);
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
