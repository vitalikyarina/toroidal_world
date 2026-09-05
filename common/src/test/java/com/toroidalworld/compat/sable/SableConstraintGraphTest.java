package com.toroidalworld.compat.sable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

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

        assertEquals(Set.of(lead, trail), Set.copyOf(graph.groupOf(lead)));
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

    @Test
    void aChainAnswersEveryMemberFromItsFarEnd() {
        SableConstraintGraph graph = new SableConstraintGraph();
        Body head = new Body();
        Body middle = new Body();
        Body tail = new Body();
        graph.record(head, middle);
        graph.record(middle, tail);

        assertEquals(Set.of(head, middle, tail), Set.copyOf(graph.groupOf(tail)));
        assertEquals(3, graph.groupOf(tail).size());
    }

    @Test
    void aCycleCountsEachMemberOnce() {
        SableConstraintGraph graph = new SableConstraintGraph();
        Body first = new Body();
        Body second = new Body();
        Body third = new Body();
        graph.record(first, second);
        graph.record(second, third);
        graph.record(third, first);

        assertEquals(3, graph.groupOf(second).size());
        assertEquals(Set.of(first, second, third), Set.copyOf(graph.groupOf(second)));
    }

    @Test
    void anIsolatedBodyAnswersItselfWhileOtherEdgesStand() {
        SableConstraintGraph graph = new SableConstraintGraph();
        Body alone = new Body();
        graph.record(new Body(), new Body());

        assertEquals(List.of(alone), graph.groupOf(alone));
    }

    @Test
    void aJointRecordedAfterAnAskJoinsTheNextAsk() {
        SableConstraintGraph graph = new SableConstraintGraph();
        Body lead = new Body();
        Body trail = new Body();
        Body newcomer = new Body();
        graph.record(lead, trail);
        assertEquals(2, graph.groupOf(lead).size());

        graph.record(trail, newcomer);

        assertEquals(Set.of(lead, trail, newcomer), Set.copyOf(graph.groupOf(lead)));
    }

    @Test
    void aDroppedEdgeSplitsTheGroupOnTheNextAsk() {
        SableConstraintGraph graph = new SableConstraintGraph();
        Body head = new Body();
        Body middle = new Body();
        Body tail = new Body();
        SableConstraintEdge headJoint = graph.record(head, middle);
        graph.record(middle, tail);
        assertEquals(3, graph.groupOf(head).size());

        headJoint.drop();

        assertEquals(List.of(head), graph.groupOf(head));
        assertEquals(Set.of(middle, tail), Set.copyOf(graph.groupOf(tail)));
    }

    @Test
    void aRemovedBodyLeavesTheRestSplitOnTheNextAsk() {
        SableConstraintGraph graph = new SableConstraintGraph();
        Body head = new Body();
        Body gone = new Body();
        Body third = new Body();
        Body tail = new Body();
        graph.record(head, gone);
        graph.record(gone, third);
        graph.record(third, tail);
        assertEquals(4, graph.groupOf(head).size());

        graph.dropBody(gone);

        assertEquals(List.of(head), graph.groupOf(head));
        assertEquals(Set.of(third, tail), Set.copyOf(graph.groupOf(third)));
    }

    @Test
    void theAnsweredGroupCannotBeWritten() {
        SableConstraintGraph graph = new SableConstraintGraph();
        Body lead = new Body();
        graph.record(lead, new Body());

        assertThrows(UnsupportedOperationException.class, () -> graph.groupOf(lead).add(new Body()));
    }
}
