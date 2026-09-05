package com.toroidalworld.compat.create;

import static com.toroidalworld.compat.CompatFoldFixture.DECK_TORUS;
import static com.toroidalworld.compat.CompatFoldFixture.PER_AXIS;
import static com.toroidalworld.compat.CompatFoldFixture.WORLD_BLOCKS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.net.TagPositions;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;

class SyncedTagFoldTest {
    private static final String PACKED_KEY = "Goal";
    private static final String BLOCK_POS_KEY = "ControllerPos";
    private static final String VEC3_KEY = "CurrentTarget";
    private static final String UNRELATED_KEY = "DesiredLength";
    private static final double UNRELATED_VALUE = 4.0;

    private static final String PRINTER_KEY = "Printer";
    private static final String ANCHOR_KEY = "Anchor";
    private static final String CURRENT_POS_KEY = "CurrentPos";
    private static final String FLYING_BLOCKS_KEY = "FlyingBlocks";
    private static final String TARGET_KEY = "Target";

    private static final BlockPos SCHEMATIC_CURSOR = new BlockPos(WORLD_BLOCKS - 4, 2, 3);

    private static final List<WorldFold> FOLDS = List.of(PER_AXIS, DECK_TORUS);

    private static final TagPositions.Table TABLE = new TagPositions.Table();

    private interface Partnered {
    }

    private static final class PackedSubject {
    }

    private static final class BlockPosSubject implements Partnered {
    }

    private static final class Vec3Subject {
    }

    private static final class UnregisteredSubject {
    }

    private static final class CannonSubject {
    }

    static {
        TABLE.register(PackedSubject.class, TagPositions.PositionShape.PACKED_LONG, PACKED_KEY);
        TABLE.register(Partnered.class, TagPositions.PositionShape.BLOCK_POS, BLOCK_POS_KEY);
        TABLE.register(Vec3Subject.class, TagPositions.PositionShape.VEC3_LIST, VEC3_KEY);
        TABLE.registerIn(CannonSubject.class, PRINTER_KEY, TagPositions.PositionShape.BLOCK_POS, ANCHOR_KEY);
        TABLE.registerInEach(CannonSubject.class, FLYING_BLOCKS_KEY, TagPositions.PositionShape.BLOCK_POS,
                TARGET_KEY);
    }

    private static CompoundTag cannonTag(BlockPos anchor, BlockPos... targets) {
        CompoundTag printer = new CompoundTag();
        printer.put(ANCHOR_KEY, NbtUtils.writeBlockPos(anchor));
        printer.put(CURRENT_POS_KEY, NbtUtils.writeBlockPos(SCHEMATIC_CURSOR));

        ListTag flying = new ListTag();
        for (BlockPos target : targets) {
            CompoundTag launched = new CompoundTag();
            launched.put(TARGET_KEY, NbtUtils.writeBlockPos(target));
            flying.add(launched);
        }

        CompoundTag tag = new CompoundTag();
        tag.put(PRINTER_KEY, printer);
        tag.put(FLYING_BLOCKS_KEY, flying);
        return tag;
    }

    private static BlockPos targetIn(CompoundTag tag, int index) {
        return NbtUtils.readBlockPos(tag.getList(FLYING_BLOCKS_KEY, Tag.TAG_COMPOUND).getCompound(index), TARGET_KEY)
                .orElseThrow();
    }

    private static ListTag doubleList(double x, double y, double z) {
        ListTag list = new ListTag();
        list.add(DoubleTag.valueOf(x));
        list.add(DoubleTag.valueOf(y));
        list.add(DoubleTag.valueOf(z));
        return list;
    }

    private static Vec3 vec3In(CompoundTag tag, String key) {
        ListTag list = tag.getList(key, Tag.TAG_DOUBLE);
        return new Vec3(list.getDouble(0), list.getDouble(1), list.getDouble(2));
    }

    @Test
    void aCannonAnchorAndItsFlyingBlocksAWorldAwayComeBackBesideTheCannon() {
        BlockPos worldPosition = new BlockPos(14, 102, 0);
        CompoundTag tag = cannonTag(new BlockPos(15 + WORLD_BLOCKS, 102, 0), new BlockPos(16 + WORLD_BLOCKS, 102, 0),
                new BlockPos(17 + WORLD_BLOCKS, 102, 0));

        for (WorldFold fold : FOLDS) {
            CompoundTag seated = SyncedTagFold.seatedIn(TABLE, fold, worldPosition, CannonSubject.class, tag);

            assertEquals(new BlockPos(15, 102, 0),
                    NbtUtils.readBlockPos(seated.getCompound(PRINTER_KEY), ANCHOR_KEY).orElseThrow(), "in " + fold);
            assertEquals(new BlockPos(16, 102, 0), targetIn(seated, 0), "in " + fold);
            assertEquals(new BlockPos(17, 102, 0), targetIn(seated, 1), "in " + fold);
        }
    }

    @Test
    void theCannonsSchematicCursorIsNotAWorldPositionAndStaysWhereItWas() {
        BlockPos worldPosition = new BlockPos(14, 102, 0);
        CompoundTag tag = cannonTag(new BlockPos(15 + WORLD_BLOCKS, 102, 0), new BlockPos(16 + WORLD_BLOCKS, 102, 0));

        for (WorldFold fold : FOLDS) {
            CompoundTag seated = SyncedTagFold.seatedIn(TABLE, fold, worldPosition, CannonSubject.class, tag);

            assertEquals(SCHEMATIC_CURSOR,
                    NbtUtils.readBlockPos(seated.getCompound(PRINTER_KEY), CURRENT_POS_KEY).orElseThrow(),
                    "in " + fold);
        }
    }

    @Test
    void aPackedLongPartnerAWorldAwayComesBackBesideTheBlockEntity() {
        BlockPos worldPosition = new BlockPos(2, 102, 0);
        CompoundTag tag = new CompoundTag();
        tag.putLong(PACKED_KEY, new BlockPos(3 + WORLD_BLOCKS, 102, 0).asLong());

        for (WorldFold fold : FOLDS) {
            CompoundTag seated = SyncedTagFold.seatedIn(TABLE, fold, worldPosition, PackedSubject.class, tag);

            assertEquals(new BlockPos(3, 102, 0), BlockPos.of(seated.getLong(PACKED_KEY)), "in " + fold);
        }
    }

    @Test
    void aBlockPosControllerAWorldAwayComesBackBesideTheBlockEntity() {
        BlockPos worldPosition = new BlockPos(6, 102, 0);
        CompoundTag tag = new CompoundTag();
        tag.put(BLOCK_POS_KEY, NbtUtils.writeBlockPos(new BlockPos(7 + WORLD_BLOCKS, 102, 0)));

        for (WorldFold fold : FOLDS) {
            CompoundTag seated = SyncedTagFold.seatedIn(TABLE, fold, worldPosition, BlockPosSubject.class, tag);

            assertEquals(new BlockPos(7, 102, 0), NbtUtils.readBlockPos(seated, BLOCK_POS_KEY).orElseThrow(),
                    "in " + fold);
        }
    }

    @Test
    void aVec3TargetAWorldAwayComesBackBesideTheBlockEntity() {
        BlockPos worldPosition = new BlockPos(10, 102, 0);
        CompoundTag tag = new CompoundTag();
        tag.put(VEC3_KEY, doubleList(0.5 + WORLD_BLOCKS, 0.5, 0.5));

        for (WorldFold fold : FOLDS) {
            CompoundTag seated = SyncedTagFold.seatedIn(TABLE, fold, worldPosition, Vec3Subject.class, tag);

            assertEquals(new Vec3(0.5, 0.5, 0.5), vec3In(seated, VEC3_KEY), "in " + fold);
        }
    }

    @Test
    void theKeysBesideAFoldedOneSurviveTheCopy() {
        BlockPos worldPosition = new BlockPos(2, 102, 0);
        CompoundTag tag = new CompoundTag();
        tag.putLong(PACKED_KEY, new BlockPos(3 + WORLD_BLOCKS, 102, 0).asLong());
        tag.putDouble(UNRELATED_KEY, UNRELATED_VALUE);

        CompoundTag seated = SyncedTagFold.seatedIn(TABLE, PER_AXIS, worldPosition, PackedSubject.class, tag);

        assertEquals(UNRELATED_VALUE, seated.getDouble(UNRELATED_KEY));
    }

    @Test
    void aSubtypeInheritsTheKeysRegisteredOnItsSupertype() {
        BlockPos worldPosition = new BlockPos(6, 102, 0);
        CompoundTag tag = new CompoundTag();
        tag.put(BLOCK_POS_KEY, NbtUtils.writeBlockPos(new BlockPos(7 + WORLD_BLOCKS, 102, 0)));

        CompoundTag seated = SyncedTagFold.seatedIn(TABLE, PER_AXIS, worldPosition, BlockPosSubject.class, tag);

        assertEquals(new BlockPos(7, 102, 0), NbtUtils.readBlockPos(seated, BLOCK_POS_KEY).orElseThrow());
    }

    @Test
    void aPositionAlreadyBesideTheBlockEntityIsTheArgumentBack() {
        BlockPos worldPosition = new BlockPos(2, 102, 0);
        CompoundTag tag = new CompoundTag();
        tag.putLong(PACKED_KEY, new BlockPos(3, 102, 0).asLong());

        assertSame(tag, SyncedTagFold.seatedIn(TABLE, PER_AXIS, worldPosition, PackedSubject.class, tag));
    }

    @Test
    void aTagWithoutTheRegisteredKeyIsTheArgumentBack() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble(UNRELATED_KEY, UNRELATED_VALUE);

        assertSame(tag, SyncedTagFold.seatedIn(TABLE, PER_AXIS, new BlockPos(2, 102, 0), PackedSubject.class, tag));
    }

    @Test
    void aValueOfAnotherShapeUnderTheRegisteredKeyIsLeftAlone() {
        CompoundTag tag = new CompoundTag();
        tag.put(VEC3_KEY, NbtUtils.writeBlockPos(new BlockPos(WORLD_BLOCKS, 0, 0)));

        assertSame(tag, SyncedTagFold.seatedIn(TABLE, PER_AXIS, new BlockPos(10, 102, 0), Vec3Subject.class, tag));
    }

    @Test
    void aTypeWithNoRegistrationIsTheArgumentBack() {
        CompoundTag tag = new CompoundTag();
        tag.putLong(PACKED_KEY, new BlockPos(3 + WORLD_BLOCKS, 102, 0).asLong());

        assertSame(tag,
                SyncedTagFold.seatedIn(TABLE, PER_AXIS, new BlockPos(2, 102, 0), UnregisteredSubject.class, tag));
    }
}
