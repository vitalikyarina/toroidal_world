package com.toroidalworld.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;

class TagPositionsTest {
    private static final int LAP_BLOCKS = 512;

    private static final String PACKED_KEY = "Goal";
    private static final String BLOCK_POS_KEY = "ControllerPos";
    private static final String VEC3_KEY = "CurrentTarget";
    private static final String TILE_X_KEY = "TileX";
    private static final String TILE_Y_KEY = "TileY";
    private static final String TILE_Z_KEY = "TileZ";
    private static final String UNRELATED_KEY = "DesiredLength";
    private static final double UNRELATED_VALUE = 4.0;

    private static final List<TagPositions.TagPosition> EVERY_SHAPE = List.of(
            new TagPositions.TagPosition(List.of(PACKED_KEY), TagPositions.PositionShape.PACKED_LONG),
            new TagPositions.TagPosition(List.of(BLOCK_POS_KEY), TagPositions.PositionShape.BLOCK_POS),
            new TagPositions.TagPosition(List.of(VEC3_KEY), TagPositions.PositionShape.VEC3_LIST),
            new TagPositions.TagPosition(List.of(TILE_X_KEY, TILE_Y_KEY, TILE_Z_KEY),
                    TagPositions.PositionShape.BLOCK_INT_TRIPLE));

    private static final class HomeLap implements TagPositions.Seat {
        private final List<String> overloads = new ArrayList<>();

        @Override
        public BlockPos seat(BlockPos stored) {
            overloads.add("BlockPos");
            return new BlockPos(homeX(stored.getX()), stored.getY(), stored.getZ());
        }

        @Override
        public Vec3 seat(Vec3 stored) {
            overloads.add("Vec3");
            return new Vec3(homeX((int) Math.floor(stored.x)) + stored.x - Math.floor(stored.x), stored.y, stored.z);
        }

        private static int homeX(int x) {
            return x - Math.floorDiv(x, LAP_BLOCKS) * LAP_BLOCKS;
        }
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

    private static void putTriple(CompoundTag tag, BlockPos pos) {
        tag.putInt(TILE_X_KEY, pos.getX());
        tag.putInt(TILE_Y_KEY, pos.getY());
        tag.putInt(TILE_Z_KEY, pos.getZ());
    }

    private static BlockPos tripleIn(CompoundTag tag) {
        return new BlockPos(tag.getInt(TILE_X_KEY), tag.getInt(TILE_Y_KEY), tag.getInt(TILE_Z_KEY));
    }

    @Test
    void everyShapeComesBackOnTheSeatedCopy() {
        CompoundTag tag = new CompoundTag();
        tag.putLong(PACKED_KEY, new BlockPos(3 + LAP_BLOCKS, 102, 0).asLong());
        tag.put(BLOCK_POS_KEY, NbtUtils.writeBlockPos(new BlockPos(7 + LAP_BLOCKS, 102, 0)));
        tag.put(VEC3_KEY, doubleList(0.5 + LAP_BLOCKS, 0.5, 0.5));
        putTriple(tag, new BlockPos(11 + LAP_BLOCKS, 102, 0));

        CompoundTag seated = TagPositions.seatedIn(new HomeLap(), EVERY_SHAPE, tag);

        assertEquals(new BlockPos(3, 102, 0), BlockPos.of(seated.getLong(PACKED_KEY)));
        assertEquals(new BlockPos(7, 102, 0), NbtUtils.readBlockPos(seated, BLOCK_POS_KEY).orElseThrow());
        assertEquals(new Vec3(0.5, 0.5, 0.5), vec3In(seated, VEC3_KEY));
        assertEquals(new BlockPos(11, 102, 0), tripleIn(seated));
    }

    @Test
    void aTripleMissingOneOfItsKeysIsLeftAlone() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(TILE_X_KEY, 11 + LAP_BLOCKS);
        tag.putInt(TILE_Z_KEY, 0);

        assertSame(tag, TagPositions.seatedIn(new HomeLap(), EVERY_SHAPE, tag));
    }

    @Test
    void aTripleTakesTheBlockPosOverloadOnce() {
        CompoundTag tag = new CompoundTag();
        putTriple(tag, new BlockPos(11 + LAP_BLOCKS, 102, 0));
        HomeLap seat = new HomeLap();

        TagPositions.seatedIn(seat, EVERY_SHAPE, tag);

        assertEquals(List.of("BlockPos"), seat.overloads);
    }

    @Test
    void aPositionCarryingTheWrongNumberOfKeysForItsShapeIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new TagPositions.TagPosition(
                List.of(TILE_X_KEY, TILE_Y_KEY), TagPositions.PositionShape.BLOCK_INT_TRIPLE));
        assertThrows(IllegalArgumentException.class, () -> new TagPositions.TagPosition(
                List.of(TILE_X_KEY, TILE_Y_KEY), TagPositions.PositionShape.BLOCK_POS));
    }

    @Test
    void aVec3KeyTakesTheVec3Overload() {
        CompoundTag tag = new CompoundTag();
        tag.put(VEC3_KEY, doubleList(0.5 + LAP_BLOCKS, 0.5, 0.5));
        HomeLap seat = new HomeLap();

        TagPositions.seatedIn(seat, EVERY_SHAPE, tag);

        assertEquals(List.of("Vec3"), seat.overloads);
    }

    @Test
    void aBlockKeyTakesTheBlockPosOverload() {
        CompoundTag tag = new CompoundTag();
        tag.putLong(PACKED_KEY, new BlockPos(3 + LAP_BLOCKS, 102, 0).asLong());
        HomeLap seat = new HomeLap();

        TagPositions.seatedIn(seat, EVERY_SHAPE, tag);

        assertEquals(List.of("BlockPos"), seat.overloads);
    }

    @Test
    void theKeysBesideASeatedOneSurviveTheCopy() {
        CompoundTag tag = new CompoundTag();
        tag.putLong(PACKED_KEY, new BlockPos(3 + LAP_BLOCKS, 102, 0).asLong());
        tag.putDouble(UNRELATED_KEY, UNRELATED_VALUE);

        CompoundTag seated = TagPositions.seatedIn(new HomeLap(), EVERY_SHAPE, tag);

        assertEquals(UNRELATED_VALUE, seated.getDouble(UNRELATED_KEY));
    }

    @Test
    void theTagHandedInIsLeftAsItWas() {
        CompoundTag tag = new CompoundTag();
        tag.putLong(PACKED_KEY, new BlockPos(3 + LAP_BLOCKS, 102, 0).asLong());

        CompoundTag seated = TagPositions.seatedIn(new HomeLap(), EVERY_SHAPE, tag);

        assertTrue(seated != tag);
        assertEquals(new BlockPos(3 + LAP_BLOCKS, 102, 0), BlockPos.of(tag.getLong(PACKED_KEY)));
    }

    @Test
    void aPositionAlreadyHomeIsTheArgumentBack() {
        CompoundTag tag = new CompoundTag();
        tag.putLong(PACKED_KEY, new BlockPos(3, 102, 0).asLong());

        assertSame(tag, TagPositions.seatedIn(new HomeLap(), EVERY_SHAPE, tag));
    }

    @Test
    void aTagWithoutAnyRegisteredKeyIsTheArgumentBack() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble(UNRELATED_KEY, UNRELATED_VALUE);

        assertSame(tag, TagPositions.seatedIn(new HomeLap(), EVERY_SHAPE, tag));
    }

    @Test
    void aValueOfAnotherShapeUnderTheKeyIsLeftAlone() {
        CompoundTag tag = new CompoundTag();
        tag.put(VEC3_KEY, NbtUtils.writeBlockPos(new BlockPos(LAP_BLOCKS, 0, 0)));

        assertSame(tag, TagPositions.seatedIn(new HomeLap(), EVERY_SHAPE, tag));
    }

    @Test
    void anEmptyPositionListIsTheArgumentBack() {
        CompoundTag tag = new CompoundTag();
        tag.putLong(PACKED_KEY, new BlockPos(3 + LAP_BLOCKS, 102, 0).asLong());

        assertSame(tag, TagPositions.seatedIn(new HomeLap(), List.of(), tag));
    }
}
