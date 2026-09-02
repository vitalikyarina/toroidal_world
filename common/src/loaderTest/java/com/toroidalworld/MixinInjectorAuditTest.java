package com.toroidalworld;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.transformer.Config;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.service.IClassProvider;
import org.spongepowered.asm.service.MixinService;

@Timeout(300)
class MixinInjectorAuditTest {
    @BeforeAll
    static void bootstrapVanilla() {
        try {
            IClassProvider classes = MixinService.getService().getClassProvider();
            classes.findClass("net.minecraft.SharedConstants", true).getMethod("tryDetectVersion").invoke(null);
            classes.findClass("net.minecraft.server.Bootstrap", true).getMethod("bootStrap").invoke(null);
        } catch (ReflectiveOperationException | LinkageError unbootstrappable) {
            Assumptions.abort("the class provider audit() force-loads through cannot bootstrap vanilla here: "
                    + unbootstrappable);
        }
    }

    @Test
    void everyInjectorTheModShipsStillMatchesTheInstructionItNames() throws IOException {
        MixinEnvironment environment = MixinEnvironment.getCurrentEnvironment();
        assertTrue(environment.getActiveTransformer() instanceof IMixinTransformer,
                "no active mixin transformer — audit() is a no-op and this test would verify nothing");

        List<String> declared = MixinConfigFixture.declaredConfigs();
        assertFalse(declared.isEmpty(), "the mod's loader metadata declares no mixin config on this classpath");

        List<String> absent = new ArrayList<>();
        for (String config : declared) {
            if (!MixinConfigFixture.isOnClasspath(config)) {
                absent.add(config);
            }
        }
        assertTrue(absent.isEmpty(), "mixin configs the mod declares but does not ship: " + absent);

        List<String> unconsumed = new ArrayList<>();
        for (Config pending : Mixins.getConfigs()) {
            if (declared.contains(pending.getName())) {
                unconsumed.add(pending.getName());
            }
        }
        assertTrue(unconsumed.isEmpty(), "declared configs the transformer never consumed: " + unconsumed);

        environment.audit();
    }
}
