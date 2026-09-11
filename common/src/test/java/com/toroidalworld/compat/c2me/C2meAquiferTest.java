package com.toroidalworld.compat.c2me;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

class C2meAquiferTest {
    private static final String CONFIG_CLASS = "com.ishland.c2me.opts.worldgen.vanilla.common.Config";
    private static final String CONFIG = CONFIG_CLASS.replace('.', '/');
    private static final String OBJECT = "java/lang/Object";

    private static final String OPTIMIZE_AQUIFER_FIELD = "optimizeAquifer";
    private static final String BOOLEAN_DESCRIPTOR = "Z";
    private static final String INT_DESCRIPTOR = "I";

    private static final int MAJOR_OVER_FEATURE = 44;
    private static final int LOADABLE_MAJOR = Runtime.version().feature() + MAJOR_OVER_FEATURE;

    private static final int INSTANCE_FIELD = 0;

    @Test
    void aRetypedSwitchKeepsTheGuessThatC2meOwnsTheAquifer() {
        assertTrue(
                C2meAquifer.readOptimizeAquifer(carrying(config(INT_DESCRIPTOR, Opcodes.ACC_STATIC))),
                "the config class loads but this C2ME build no longer declares the switch a boolean");
    }

    @Test
    void aNonStaticSwitchKeepsTheGuessThatC2meOwnsTheAquifer() {
        assertTrue(
                C2meAquifer.readOptimizeAquifer(carrying(config(BOOLEAN_DESCRIPTOR, INSTANCE_FIELD))),
                "the config class loads but this C2ME build no longer declares the switch static");
    }

    private static byte[] config(String switchDescriptor, int switchFlags) {
        ClassWriter config = new ClassWriter(0);
        config.visit(LOADABLE_MAJOR, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, CONFIG, null, OBJECT, null);
        config.visitField(Opcodes.ACC_PUBLIC | switchFlags, OPTIMIZE_AQUIFER_FIELD, switchDescriptor, null, null)
                .visitEnd();
        config.visitEnd();
        return config.toByteArray();
    }

    private static ClassLoader carrying(byte[] config) {
        return new ClassLoader(null) {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                if (!CONFIG_CLASS.equals(name)) {
                    throw new ClassNotFoundException(name);
                }

                return defineClass(name, config, 0, config.length);
            }
        };
    }
}
