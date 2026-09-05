package com.toroidalworld.compat.aeronautics;

import static com.toroidalworld.compat.CompatFoldFixture.PER_AXIS;
import static com.toroidalworld.compat.CompatFoldFixture.WORLD_BLOCKS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WorldFolds;

import dev.simulated_team.simulated.content.blocks.rope.strand.server.RopeAttachment;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.RopeAttachmentPoint;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

class RopeSeamFrameTest {
    private static final double ROPE_Y = 70.0;
    private static final double ROPE_Z = 10.0;
    private static final double BODY_Y = 80.0;

    private static final UUID BODY_AT_START = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BODY_AT_END = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID BODY_ELSEWHERE = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final BlockPos ANY_BLOCK = new BlockPos(250, 70, 10);

    private static final RopeAttachment START_ON_A_BLOCK =
            new RopeAttachment(RopeAttachmentPoint.START, null, ANY_BLOCK);
    private static final RopeAttachment END_ON_A_BLOCK =
            new RopeAttachment(RopeAttachmentPoint.END, null, ANY_BLOCK);
    private static final RopeAttachment START_ON_A_BODY =
            new RopeAttachment(RopeAttachmentPoint.START, BODY_AT_START, ANY_BLOCK);
    private static final RopeAttachment END_ON_A_BODY =
            new RopeAttachment(RopeAttachmentPoint.END, BODY_AT_END, ANY_BLOCK);

    private static final List<Vector3d> NEAR_SIDE_STRAND = chain(250.0, 252.0, 254.0);
    private static final List<Vector3d> FAR_SIDE_STRAND = chain(-254.0, -252.0, -250.0);

    private static List<Vector3d> chain(double... xs) {
        List<Vector3d> points = new ArrayList<>();
        for (double x : xs) {
            points.add(new Vector3d(x, ROPE_Y, ROPE_Z));
        }
        return points;
    }

    private static Vector3d at(double x) {
        return new Vector3d(x, ROPE_Y, ROPE_Z);
    }

    private static Vec3 bodyAt(double x) {
        return new Vec3(x, BODY_Y, ROPE_Z);
    }

    @Test
    void anAttachmentAcrossTheSeamIsSeatedOnTheStrandsSide() {
        assertEquals(at(-254.0 + WORLD_BLOCKS),
                RopeSeamFrame.seatAttachment(PER_AXIS, NEAR_SIDE_STRAND, END_ON_A_BLOCK, at(-254.0)));
        assertEquals(at(254.0 - WORLD_BLOCKS),
                RopeSeamFrame.seatAttachment(PER_AXIS, FAR_SIDE_STRAND, START_ON_A_BLOCK, at(254.0)));
    }

    @Test
    void theSeatAnchorsOnTheEndTheAttachmentNames() {
        Vector3d fartherThanHalfFromTheEndOnly = at(-4.0);

        assertEquals(at(-4.0 + WORLD_BLOCKS),
                RopeSeamFrame.seatAttachment(PER_AXIS, NEAR_SIDE_STRAND, END_ON_A_BLOCK, fartherThanHalfFromTheEndOnly));
        assertSame(fartherThanHalfFromTheEndOnly, RopeSeamFrame.seatAttachment(PER_AXIS, NEAR_SIDE_STRAND,
                START_ON_A_BLOCK, fartherThanHalfFromTheEndOnly));
    }

    @Test
    void anAttachmentOnASubLevelIsGivenBackByIdentity() {
        Vector3d across = at(-254.0);

        assertSame(across, RopeSeamFrame.seatAttachment(PER_AXIS, NEAR_SIDE_STRAND, END_ON_A_BODY, across));
    }

    @Test
    void anUnwrappedWorldAndAnEmptyStrandGiveTheAttachmentBackByIdentity() {
        Vector3d across = at(-254.0);

        assertSame(across, RopeSeamFrame.seatAttachment(null, NEAR_SIDE_STRAND, END_ON_A_BLOCK, across));
        assertSame(across, RopeSeamFrame.seatAttachment(WorldFolds.NOOP, NEAR_SIDE_STRAND, END_ON_A_BLOCK, across));
        assertSame(across, RopeSeamFrame.seatAttachment(PER_AXIS, List.of(), END_ON_A_BLOCK, across));
    }

    @Test
    void aStrandBelongsToTheGroupThroughEitherAttachmentBody() {
        List<RopeAttachment> attachments = List.of(START_ON_A_BODY, END_ON_A_BODY);

        assertSame(START_ON_A_BODY, RopeSeamFrame.shiftedAttachmentOf(attachments, Set.of(BODY_AT_START)));
        assertSame(END_ON_A_BODY, RopeSeamFrame.shiftedAttachmentOf(attachments, Set.of(BODY_AT_END)));
        assertNull(RopeSeamFrame.shiftedAttachmentOf(attachments, Set.of(BODY_ELSEWHERE)));
    }

    @Test
    void anAttachmentOnABlockNeverJoinsAShiftedGroup() {
        assertNull(RopeSeamFrame.shiftedAttachmentOf(List.of(START_ON_A_BLOCK, END_ON_A_BLOCK),
                Set.of(BODY_AT_START, BODY_AT_END)));
    }

    @Test
    void aLappedBodyMovesTheWholeChainByOneLapAndOnlyOnce() {
        List<Vector3d> chain = chain(-10.0, 50.0, 110.0, 170.0, 230.0, 250.0);
        Vec3 bodyAfterItsLap = bodyAt(251.5 - WORLD_BLOCKS);

        assertTrue(RopeSeamFrame.reseat(PER_AXIS, chain, END_ON_A_BODY, bodyAfterItsLap));
        assertEquals(chain(-10.0 - WORLD_BLOCKS, 50.0 - WORLD_BLOCKS, 110.0 - WORLD_BLOCKS, 170.0 - WORLD_BLOCKS,
                230.0 - WORLD_BLOCKS, 250.0 - WORLD_BLOCKS), chain);

        assertFalse(RopeSeamFrame.reseat(PER_AXIS, chain, END_ON_A_BODY, bodyAfterItsLap));
        assertEquals(at(250.0 - WORLD_BLOCKS), chain.getLast());
    }

    @Test
    void theReseatFollowsTheEndTheAttachmentNames() {
        List<Vector3d> chain = chain(240.0, 250.0);

        assertTrue(RopeSeamFrame.reseat(PER_AXIS, chain, START_ON_A_BODY, bodyAt(239.5 - WORLD_BLOCKS)));
        assertEquals(chain(240.0 - WORLD_BLOCKS, 250.0 - WORLD_BLOCKS), chain);
    }

    @Test
    void aBodyStillOnTheStrandsSideLeavesTheChainWhereItIs() {
        List<Vector3d> chain = chain(240.0, 250.0);

        assertFalse(RopeSeamFrame.reseat(PER_AXIS, chain, END_ON_A_BODY, bodyAt(251.5)));
        assertFalse(RopeSeamFrame.reseat(null, chain, END_ON_A_BODY, bodyAt(251.5 - WORLD_BLOCKS)));
        assertEquals(chain(240.0, 250.0), chain);
    }
}
