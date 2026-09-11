package com.toroidalworld.compat.c2me;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

class C2meNativesMathTest {
    private static final String ENTRY_POINT_CLASS = "com.ishland.c2me.opts.natives_math.ModuleEntryPoint";
    private static final String ENTRY_POINT = ENTRY_POINT_CLASS.replace('.', '/');
    private static final String OBJECT = "java/lang/Object";
    private static final String BOOLEAN_DESCRIPTOR = "Z";

    private static final String ENABLED_FIELD = "enabled";
    private static final String RENAMED_FIELD = "active";

    private static final int MAJOR_OVER_FEATURE = 44;
    private static final int LOADABLE_MAJOR = Runtime.version().feature() + MAJOR_OVER_FEATURE;
    private static final int NEWER_JAVA_MAJOR = LOADABLE_MAJOR + 1;

    @Test
    void aModuleThatReadsOnOwnsTheEndIslands() {
        assertTrue(C2meNativesMath.readEnabled(carrying(entryPoint(ENABLED_FIELD, true, LOADABLE_MAJOR))),
                "C2ME is installed and its natives_math switch reads on");
    }

    @Test
    void aModuleThatReadsOffLeavesTheEndIslandsToUs() {
        assertFalse(C2meNativesMath.readEnabled(carrying(entryPoint(ENABLED_FIELD, false, LOADABLE_MAJOR))),
                "C2ME is installed and its natives_math switch reads off");
    }

    @Test
    void anUninstalledC2meLeavesTheEndIslandsToUs() {
        assertFalse(C2meNativesMath.readEnabled(carryingNothing()),
                "no jar on the classpath carries the natives_math entry point");
    }

    @Test
    void aRenamedSwitchKeepsTheGuessThatC2meOwnsTheEndIslands() {
        assertTrue(C2meNativesMath.readEnabled(carrying(entryPoint(RENAMED_FIELD, false, LOADABLE_MAJOR))),
                "the entry point loads but this C2ME build no longer declares the field the gate reads");
    }

    @Test
    void aModuleCompiledForANewerJavaCannotOwnTheEndIslands() {
        assertFalse(C2meNativesMath.readEnabled(carrying(entryPoint(ENABLED_FIELD, true, NEWER_JAVA_MAJOR))),
                "the entry point cannot load on this runtime, so the module it belongs to is not running");
    }

    private static byte[] entryPoint(String switchField, boolean value, int major) {
        ClassWriter entry = new ClassWriter(0);
        entry.visit(major, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, ENTRY_POINT, null, OBJECT, null);
        entry.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, switchField,
                BOOLEAN_DESCRIPTOR, null, value ? 1 : 0).visitEnd();
        entry.visitEnd();
        return entry.toByteArray();
    }

    private static ClassLoader carrying(byte[] entryPoint) {
        return new ClassLoader(null) {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                if (!ENTRY_POINT_CLASS.equals(name)) {
                    throw new ClassNotFoundException(name);
                }

                return defineClass(name, entryPoint, 0, entryPoint.length);
            }
        };
    }

    private static ClassLoader carryingNothing() {
        return new ClassLoader(null) {
        };
    }
}
