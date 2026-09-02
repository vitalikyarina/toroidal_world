package com.toroidalworld;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.transformer.Config;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

@Timeout(300)
class MixinInjectorAuditTest {
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
