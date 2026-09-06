package com.toroidalworld.compat.aeronautics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class BundleModTest {
    private static final String[] SIMULATED_TARGETS = {
            "dev.simulated_team.simulated.content.blocks.redstone.AbstractLinkedReceiverBlockEntity",
            "dev.simulated_team.simulated.util.hold_interaction.BlockHoldInteraction",
            "dev.simulated_team.simulated.content.entities.diagram.DiagramEntity",
            "dev.simulated_team.simulated.content.blocks.docking_connector.DockingConnectorPair",
            "dev.simulated_team.simulated.content.blocks.merging_glue.MergingGlueBlockEntity",
            "dev.simulated_team.simulated.content.blocks.spring.SpringBlockEntity",
            "dev.simulated_team.simulated.content.blocks.redstone_magnet.MagnetMap",
            "dev.simulated_team.simulated.content.blocks.redstone_magnet.MagnetPair",
            "dev.simulated_team.simulated.content.blocks.redstone_magnet.RedstoneMagnetBlockEntity",
            "dev.simulated_team.simulated.content.navigation_targets.MapNavigationTarget",
            "dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity",
            "dev.simulated_team.simulated.network.packets.physics_staff.PhysicsStaffBeamPacket",
            "dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior",
            "dev.simulated_team.simulated.content.blocks.rope.strand.server.ServerRopeStrand",
            "dev.simulated_team.simulated.content.items.rope.RopeItem$ClientRopeItemHandler"
    };

    private static final String OFFROAD_TARGET =
            "dev.ryanhcode.offroad.network.borehead_bearing.ClientboundMultiMiningSync";

    private static final String AERONAUTICS_TARGET = "dev.eriksonn.aeronautics.Aeronautics";

    private static final String NESTED_SIMULATED_TARGET =
            "dev.simulated_team.simulated.content.items.rope.RopeItem$ClientRopeItemHandler";

    private static final String FOREIGN_TARGET = "net.minecraft.world.level.Level";

    @Test
    void everyListedSimulatedTargetIsOwnedBySimulated() {
        for (String target : SIMULATED_TARGETS) {
            assertEquals(BundleMod.SIMULATED, BundleMod.owning(target), target);
        }
    }

    @Test
    void theListedOffroadTargetIsOwnedByOffroad() {
        assertEquals(BundleMod.OFFROAD, BundleMod.owning(OFFROAD_TARGET),
                "the one listed target outside the Simulated package belongs to Offroad");
    }

    @Test
    void anAeronauticsTargetIsOwnedByAeronautics() {
        assertEquals(BundleMod.AERONAUTICS, BundleMod.owning(AERONAUTICS_TARGET),
                "the bundle's third mod owns its own package, with no mixin listed against it yet");
    }

    @Test
    void aNestedTargetIsOwnedThroughTheOuterClassPackage() {
        assertEquals(BundleMod.SIMULATED, BundleMod.owning(NESTED_SIMULATED_TARGET),
                "a nested target's runtime name carries $ after the outer class, leaving the package prefix intact");
    }

    @Test
    void aTargetOutsideTheBundleIsOwnedByNoOne() {
        assertNull(BundleMod.owning(FOREIGN_TARGET),
                "no bundle mod claims the target, so the gate refuses instead of defaulting to one");
    }
}
