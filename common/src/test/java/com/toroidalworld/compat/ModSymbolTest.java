package com.toroidalworld.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ModSymbolTest {
    private static final String SHIPPED_OWNER = "com/toroidalworld/compat/ModPresence";
    private static final String ABSENT_OWNER = "com/toroidalworld/compat/NoSuchModEntryPoint";

    private static final ClassLoader LOADER = ModSymbolTest.class.getClassLoader();

    @Test
    void aFieldTheClassDeclaresReadsAsCarried() {
        assertTrue(new ModSymbol(SHIPPED_OWNER, "gateLabel", "Ljava/lang/String;").carriedBy(LOADER),
                "the class file lists gateLabel among its fields");
    }

    @Test
    void aMethodTheClassDeclaresReadsAsCarried() {
        assertTrue(new ModSymbol(SHIPPED_OWNER, "probe", "(Ljava/lang/String;)Z").carriedBy(LOADER),
                "the class file lists the single-argument probe among its methods");
    }

    @Test
    void aMemberWithAnotherDescriptorReadsAsAbsent() {
        assertFalse(new ModSymbol(SHIPPED_OWNER, "probe", "(I)Z").carriedBy(LOADER),
                "probe is declared, but never over an int");
    }

    @Test
    void aMemberTheClassNeverDeclaredReadsAsAbsent() {
        assertFalse(new ModSymbol(SHIPPED_OWNER, "centerBlockX", "D").carriedBy(LOADER),
                "the class carries no such member under any descriptor");
    }

    @Test
    void aClassNothingShipsReadsAsAbsent() {
        assertFalse(new ModSymbol(ABSENT_OWNER, "probe", "(Ljava/lang/String;)Z").carriedBy(LOADER),
                "no jar on the classpath carries that class file");
    }

    @Test
    void theSymbolNamesItselfAsTheGateLogsIt() {
        assertEquals("com/toroidalworld/compat/ModPresence#probe:(Ljava/lang/String;)Z",
                new ModSymbol(SHIPPED_OWNER, "probe", "(Ljava/lang/String;)Z").toString(),
                "a refused gate has to name owner, member and descriptor in one value");
    }
}
