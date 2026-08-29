package com.toroidalworld.compat.create;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.DeckGroupFold;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.net.TagPositions;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;

class SyncedTagFoldTest {
    private static final int WORLD_CHUNKS = 16;
    private static final int WORLD_BLOCKS = WORLD_CHUNKS * 2 * 16;

    private static final String PACKED_KEY = "Goal";
    private static final String BLOCK_POS_KEY = "ControllerPos";
    private static final String VEC3_KEY = "CurrentTarget";
    private static final String UNRELATED_KEY = "DesiredLength";
    private static final double UNRELATED_VALUE = 4.0;

    private static final WorldLoopBounds BOUNDS =
            new WorldLoopBounds(-WORLD_CHUNKS, WORLD_CHUNKS, -WORLD_CHUNKS, WORLD_CHUNKS);

    private static final WorldFold PER_AXIS = WorldFolds.of(FlatShape.latticeTorus(BOUNDS, FlatShape.NO_SKEW));
    private static final WorldFold DECK_TORUS = new DeckGroupFold(FlatShape.latticeTorus(BOUNDS, FlatShape.NO_SKEW));
    private static final List<WorldFold> FOLDS = List.of(PER_AXIS, DECK_TORUS);

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

    static {
        SyncedTagFold.register(PackedSubject.class, TagPositions.PositionShape.PACKED_LONG, PACKED_KEY);
        SyncedTagFold.register(Partnered.class, TagPositions.PositionShape.BLOCK_POS, BLOCK_POS_KEY);
        SyncedTagFold.register(Vec3Subject.class, TagPositions.PositionShape.VEC3_LIST, VEC3_KEY);
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
    void aPackedLongPartnerAWorldAwayComesBackBesideTheBlockEntity() {
        BlockPos worldPosition = new BlockPos(2, 102, 0);
        CompoundTag tag = new CompoundTag();
        tag.putLong(PACKED_KEY, new BlockPos(3 + WORLD_BLOCKS, 102, 0).asLong());

        for (WorldFold fold : FOLDS) {
            CompoundTag seated = SyncedTagFold.seatedIn(fold, worldPosition, PackedSubject.class, tag);

            assertEquals(new BlockPos(3, 102, 0), BlockPos.of(seated.getLong(PACKED_KEY)), "in " + fold);
        }
    }

    @Test
    void aBlockPosControllerAWorldAwayComesBackBesideTheBlockEntity() {
        BlockPos worldPosition = new BlockPos(6, 102, 0);
        CompoundTag tag = new CompoundTag();
        tag.put(BLOCK_POS_KEY, NbtUtils.writeBlockPos(new BlockPos(7 + WORLD_BLOCKS, 102, 0)));

        for (WorldFold fold : FOLDS) {
            CompoundTag seated = SyncedTagFold.seatedIn(fold, worldPosition, BlockPosSubject.class, tag);

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
            CompoundTag seated = SyncedTagFold.seatedIn(fold, worldPosition, Vec3Subject.class, tag);

            assertEquals(new Vec3(0.5, 0.5, 0.5), vec3In(seated, VEC3_KEY), "in " + fold);
        }
    }

    @Test
    void theKeysBesideAFoldedOneSurviveTheCopy() {
        BlockPos worldPosition = new BlockPos(2, 102, 0);
        CompoundTag tag = new CompoundTag();
        tag.putLong(PACKED_KEY, new BlockPos(3 + WORLD_BLOCKS, 102, 0).asLong());
        tag.putDouble(UNRELATED_KEY, UNRELATED_VALUE);

        CompoundTag seated = SyncedTagFold.seatedIn(PER_AXIS, worldPosition, PackedSubject.class, tag);

        assertEquals(UNRELATED_VALUE, seated.getDouble(UNRELATED_KEY));
    }

    @Test
    void aSubtypeInheritsTheKeysRegisteredOnItsSupertype() {
        BlockPos worldPosition = new BlockPos(6, 102, 0);
        CompoundTag tag = new CompoundTag();
        tag.put(BLOCK_POS_KEY, NbtUtils.writeBlockPos(new BlockPos(7 + WORLD_BLOCKS, 102, 0)));

        CompoundTag seated = SyncedTagFold.seatedIn(PER_AXIS, worldPosition, BlockPosSubject.class, tag);

        assertEquals(new BlockPos(7, 102, 0), NbtUtils.readBlockPos(seated, BLOCK_POS_KEY).orElseThrow());
    }

    @Test
    void aPositionAlreadyBesideTheBlockEntityIsTheArgumentBack() {
        BlockPos worldPosition = new BlockPos(2, 102, 0);
        CompoundTag tag = new CompoundTag();
        tag.putLong(PACKED_KEY, new BlockPos(3, 102, 0).asLong());

        assertSame(tag, SyncedTagFold.seatedIn(PER_AXIS, worldPosition, PackedSubject.class, tag));
    }

    @Test
    void aTagWithoutTheRegisteredKeyIsTheArgumentBack() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble(UNRELATED_KEY, UNRELATED_VALUE);

        assertSame(tag, SyncedTagFold.seatedIn(PER_AXIS, new BlockPos(2, 102, 0), PackedSubject.class, tag));
    }

    @Test
    void aValueOfAnotherShapeUnderTheRegisteredKeyIsLeftAlone() {
        CompoundTag tag = new CompoundTag();
        tag.put(VEC3_KEY, NbtUtils.writeBlockPos(new BlockPos(WORLD_BLOCKS, 0, 0)));

        assertSame(tag, SyncedTagFold.seatedIn(PER_AXIS, new BlockPos(10, 102, 0), Vec3Subject.class, tag));
    }

    @Test
    void aTypeWithNoRegistrationIsTheArgumentBack() {
        CompoundTag tag = new CompoundTag();
        tag.putLong(PACKED_KEY, new BlockPos(3 + WORLD_BLOCKS, 102, 0).asLong());

        assertSame(tag, SyncedTagFold.seatedIn(PER_AXIS, new BlockPos(2, 102, 0), UnregisteredSubject.class, tag));
    }
}
