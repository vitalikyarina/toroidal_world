package com.toroidalworld.compat.c2me;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.classfile.ClassFile;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;

import org.junit.jupiter.api.Test;

class C2meAquiferTest {
    private static final String CONFIG_CLASS = "com.ishland.c2me.opts.worldgen.vanilla.common.Config";
    private static final ClassDesc CONFIG = ClassDesc.of(CONFIG_CLASS);

    private static final String OPTIMIZE_AQUIFER_FIELD = "optimizeAquifer";

    private static final int INSTANCE_FIELD = 0;

    @Test
    void aRetypedSwitchKeepsTheGuessThatC2meOwnsTheAquifer() {
        assertTrue(
                C2meAquifer.readOptimizeAquifer(
                        carrying(config(ConstantDescs.CD_int, ClassFile.ACC_STATIC))),
                "the config class loads but this C2ME build no longer declares the switch a boolean");
    }

    @Test
    void aNonStaticSwitchKeepsTheGuessThatC2meOwnsTheAquifer() {
        assertTrue(
                C2meAquifer.readOptimizeAquifer(
                        carrying(config(ConstantDescs.CD_boolean, INSTANCE_FIELD))),
                "the config class loads but this C2ME build no longer declares the switch static");
    }

    private static byte[] config(ClassDesc switchType, int switchFlags) {
        return ClassFile.of().build(CONFIG, config -> config
                .withVersion(ClassFile.latestMajorVersion(), 0)
                .withFlags(ClassFile.ACC_PUBLIC | ClassFile.ACC_FINAL)
                .withField(OPTIMIZE_AQUIFER_FIELD, switchType, field -> field
                        .withFlags(ClassFile.ACC_PUBLIC | switchFlags)));
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
