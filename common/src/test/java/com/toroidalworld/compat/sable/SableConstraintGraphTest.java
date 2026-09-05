package com.toroidalworld.compat.sable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.api.physics.mass.MassData;

class SableConstraintGraphTest {
    private static final class Body implements PhysicsPipelineBody {
        @Override
        public int getRuntimeId() {
            return NULL_RUNTIME_ID;
        }

        @Override
        public MassData getMassTracker() {
            return null;
        }

        @Override
        public boolean isRemoved() {
            return false;
        }
    }

    @Test
    void anEdgeLeavesTheStoreWithItsConstraintAndNoReadInBetween() {
        SableConstraintGraph graph = new SableConstraintGraph();
        SableConstraintEdge edge = graph.record(new Body(), new Body());
        assertFalse(graph.isEmpty());

        edge.drop();

        assertTrue(graph.isEmpty(), "nothing walked the store between record and drop");
    }

    @Test
    void droppingAnEdgeTwiceLeavesTheOthersAlone() {
        SableConstraintGraph graph = new SableConstraintGraph();
        Body lead = new Body();
        Body trail = new Body();
        SableConstraintEdge gone = graph.record(new Body(), new Body());
        graph.record(lead, trail);

        gone.drop();
        gone.drop();

        assertEquals(List.of(lead, trail), graph.groupOf(lead));
    }

    @Test
    void aRemovedBodyTakesEveryEdgeItTouches() {
        SableConstraintGraph graph = new SableConstraintGraph();
        Body shared = new Body();
        graph.record(shared, new Body());
        graph.record(new Body(), shared);

        graph.dropBody(shared);

        assertTrue(graph.isEmpty());
    }

    @Test
    void aDroppedEdgeNoLongerJoinsTheGroup() {
        SableConstraintGraph graph = new SableConstraintGraph();
        Body lead = new Body();
        Body trail = new Body();
        SableConstraintEdge edge = graph.record(lead, trail);
        assertEquals(2, graph.groupOf(lead).size());

        edge.drop();

        assertEquals(List.of(lead), graph.groupOf(lead));
    }
}
