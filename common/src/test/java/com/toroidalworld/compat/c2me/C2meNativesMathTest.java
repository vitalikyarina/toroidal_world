package com.toroidalworld.compat.c2me;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.classfile.ClassFile;
import java.lang.classfile.attribute.ConstantValueAttribute;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;

import org.junit.jupiter.api.Test;

class C2meNativesMathTest {
    private static final String ENTRY_POINT_CLASS = "com.ishland.c2me.opts.natives_math.ModuleEntryPoint";
    private static final ClassDesc ENTRY_POINT = ClassDesc.of(ENTRY_POINT_CLASS);

    private static final String ENABLED_FIELD = "enabled";
    private static final String RENAMED_FIELD = "active";

    private static final int LOADABLE_MAJOR = ClassFile.latestMajorVersion();
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
        return ClassFile.of().build(ENTRY_POINT, entry -> entry
                .withVersion(major, 0)
                .withFlags(ClassFile.ACC_PUBLIC | ClassFile.ACC_FINAL)
                .withField(switchField, ConstantDescs.CD_boolean, field -> field
                        .withFlags(ClassFile.ACC_PUBLIC | ClassFile.ACC_STATIC | ClassFile.ACC_FINAL)
                        .with(ConstantValueAttribute.of(value ? 1 : 0))));
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
