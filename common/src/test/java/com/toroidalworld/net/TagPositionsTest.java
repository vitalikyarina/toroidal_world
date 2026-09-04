package com.toroidalworld.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Nested;
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
    private static final String COMPOUND_KEY = "Printer";
    private static final String LIST_KEY = "FlyingBlocks";

    private static final List<TagPositions.TagPosition> EVERY_SHAPE = List.of(
            new TagPositions.TagPosition(List.of(PACKED_KEY), TagPositions.PositionShape.PACKED_LONG),
            new TagPositions.TagPosition(List.of(BLOCK_POS_KEY), TagPositions.PositionShape.BLOCK_POS),
            new TagPositions.TagPosition(List.of(VEC3_KEY), TagPositions.PositionShape.VEC3_LIST),
            new TagPositions.TagPosition(List.of(TILE_X_KEY, TILE_Y_KEY, TILE_Z_KEY),
                    TagPositions.PositionShape.BLOCK_INT_TRIPLE));

    private static final List<TagPositions.TagPosition> EVERY_SHAPE_IN_COMPOUND =
            addressed(TagPositions.Nesting.COMPOUND, COMPOUND_KEY);
    private static final List<TagPositions.TagPosition> EVERY_SHAPE_IN_EACH_OF_LIST =
            addressed(TagPositions.Nesting.EACH_OF_LIST, LIST_KEY);

    private interface Anchored {
    }

    private static final class AnchoredSubject implements Anchored {
    }

    private static class Hung {
    }

    private static final class HungSubject extends Hung {
    }

    private static final class PackedSubject {
    }

    private static final class UnregisteredSubject {
    }

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

    private static List<TagPositions.TagPosition> addressed(TagPositions.Nesting nesting, String container) {
        return EVERY_SHAPE.stream()
                .map(position -> new TagPositions.TagPosition(nesting, container, position.keys(), position.shape()))
                .toList();
    }

    private static CompoundTag everyShapeALapOut() {
        CompoundTag tag = new CompoundTag();
        tag.putLong(PACKED_KEY, new BlockPos(3 + LAP_BLOCKS, 102, 0).asLong());
        tag.put(BLOCK_POS_KEY, NbtUtils.writeBlockPos(new BlockPos(7 + LAP_BLOCKS, 102, 0)));
        tag.put(VEC3_KEY, doubleList(0.5 + LAP_BLOCKS, 0.5, 0.5));
        putTriple(tag, new BlockPos(11 + LAP_BLOCKS, 102, 0));
        return tag;
    }

    private static void assertEveryShapeHome(CompoundTag tag, String where) {
        assertEquals(new BlockPos(3, 102, 0), BlockPos.of(tag.getLong(PACKED_KEY)), where);
        assertEquals(new BlockPos(7, 102, 0), NbtUtils.readBlockPos(tag, BLOCK_POS_KEY).orElseThrow(), where);
        assertEquals(new Vec3(0.5, 0.5, 0.5), vec3In(tag, VEC3_KEY), where);
        assertEquals(new BlockPos(11, 102, 0), tripleIn(tag), where);
    }

    private static CompoundTag blockPosAt(BlockPos position) {
        CompoundTag tag = new CompoundTag();
        tag.put(BLOCK_POS_KEY, NbtUtils.writeBlockPos(position));
        return tag;
    }

    @Test
    void everyShapeComesBackOnTheSeatedCopy() {
        CompoundTag tag = everyShapeALapOut();

        assertEveryShapeHome(TagPositions.seatedIn(new HomeLap(), EVERY_SHAPE, tag), "top level");
    }

    @Test
    void everyShapeInsideANestedCompoundComesBackOnTheSeatedCopy() {
        CompoundTag printer = everyShapeALapOut();
        printer.putDouble(UNRELATED_KEY, UNRELATED_VALUE);
        CompoundTag tag = new CompoundTag();
        tag.put(COMPOUND_KEY, printer);
        tag.putDouble(UNRELATED_KEY, UNRELATED_VALUE);

        CompoundTag seated = TagPositions.seatedIn(new HomeLap(), EVERY_SHAPE_IN_COMPOUND, tag);

        assertEveryShapeHome(seated.getCompound(COMPOUND_KEY), COMPOUND_KEY);
        assertEquals(UNRELATED_VALUE, seated.getCompound(COMPOUND_KEY).getDouble(UNRELATED_KEY));
        assertEquals(UNRELATED_VALUE, seated.getDouble(UNRELATED_KEY));
    }

    @Test
    void everyShapeInEachCompoundOfAListComesBackOnTheSeatedCopy() {
        CompoundTag second = everyShapeALapOut();
        second.putDouble(UNRELATED_KEY, UNRELATED_VALUE);
        ListTag launched = new ListTag();
        launched.add(everyShapeALapOut());
        launched.add(second);
        CompoundTag tag = new CompoundTag();
        tag.put(LIST_KEY, launched);

        CompoundTag seated = TagPositions.seatedIn(new HomeLap(), EVERY_SHAPE_IN_EACH_OF_LIST, tag);

        ListTag seatedList = seated.getList(LIST_KEY, Tag.TAG_COMPOUND);
        assertEveryShapeHome(seatedList.getCompound(0), LIST_KEY + "[0]");
        assertEveryShapeHome(seatedList.getCompound(1), LIST_KEY + "[1]");
        assertEquals(UNRELATED_VALUE, seatedList.getCompound(1).getDouble(UNRELATED_KEY));
    }

    @Test
    void theElementsBesideAFoldedOneSurviveTheList() {
        ListTag launched = new ListTag();
        launched.add(blockPosAt(new BlockPos(7, 102, 0)));
        launched.add(blockPosAt(new BlockPos(9 + LAP_BLOCKS, 102, 0)));
        CompoundTag tag = new CompoundTag();
        tag.put(LIST_KEY, launched);

        CompoundTag seated = TagPositions.seatedIn(new HomeLap(), EVERY_SHAPE_IN_EACH_OF_LIST, tag);

        ListTag seatedList = seated.getList(LIST_KEY, Tag.TAG_COMPOUND);
        assertEquals(new BlockPos(7, 102, 0),
                NbtUtils.readBlockPos(seatedList.getCompound(0), BLOCK_POS_KEY).orElseThrow());
        assertEquals(new BlockPos(9, 102, 0),
                NbtUtils.readBlockPos(seatedList.getCompound(1), BLOCK_POS_KEY).orElseThrow());
    }

    @Test
    void aNestedPositionAlreadyHomeIsTheArgumentBack() {
        CompoundTag tag = new CompoundTag();
        tag.put(COMPOUND_KEY, blockPosAt(new BlockPos(7, 102, 0)));

        assertSame(tag, TagPositions.seatedIn(new HomeLap(), EVERY_SHAPE_IN_COMPOUND, tag));
    }

    @Test
    void aListedPositionAlreadyHomeIsTheArgumentBack() {
        ListTag launched = new ListTag();
        launched.add(blockPosAt(new BlockPos(7, 102, 0)));
        CompoundTag tag = new CompoundTag();
        tag.put(LIST_KEY, launched);

        assertSame(tag, TagPositions.seatedIn(new HomeLap(), EVERY_SHAPE_IN_EACH_OF_LIST, tag));
    }

    @Test
    void aTagWithoutTheNamedContainerIsTheArgumentBack() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble(UNRELATED_KEY, UNRELATED_VALUE);

        assertSame(tag, TagPositions.seatedIn(new HomeLap(), EVERY_SHAPE_IN_COMPOUND, tag));
        assertSame(tag, TagPositions.seatedIn(new HomeLap(), EVERY_SHAPE_IN_EACH_OF_LIST, tag));
    }

    @Test
    void anAddressWithoutItsContainerIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new TagPositions.TagPosition(
                TagPositions.Nesting.COMPOUND, null, List.of(BLOCK_POS_KEY), TagPositions.PositionShape.BLOCK_POS));
        assertThrows(IllegalArgumentException.class, () -> new TagPositions.TagPosition(
                TagPositions.Nesting.TOP, COMPOUND_KEY, List.of(BLOCK_POS_KEY),
                TagPositions.PositionShape.BLOCK_POS));
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

    @Nested
    class Tables {
        @Test
        void aTypeCarriesTheKeysRegisteredOnItAndOnEverySupertype() {
            TagPositions.Table table = new TagPositions.Table();
            table.register(Anchored.class, TagPositions.PositionShape.BLOCK_POS, BLOCK_POS_KEY);
            table.register(AnchoredSubject.class, TagPositions.PositionShape.PACKED_LONG, PACKED_KEY);

            CompoundTag tag = new CompoundTag();
            tag.put(BLOCK_POS_KEY, NbtUtils.writeBlockPos(new BlockPos(7 + LAP_BLOCKS, 102, 0)));
            tag.putLong(PACKED_KEY, new BlockPos(3 + LAP_BLOCKS, 102, 0).asLong());

            CompoundTag seated = table.seatedIn(new HomeLap(), AnchoredSubject.class, tag);

            assertEquals(new BlockPos(7, 102, 0), NbtUtils.readBlockPos(seated, BLOCK_POS_KEY).orElseThrow());
            assertEquals(new BlockPos(3, 102, 0), BlockPos.of(seated.getLong(PACKED_KEY)));
        }

        @Test
        void aTypeCarriesTheKeysRegisteredOnItsSuperclass() {
            TagPositions.Table table = new TagPositions.Table();
            table.register(Hung.class, TagPositions.PositionShape.BLOCK_POS, BLOCK_POS_KEY);

            CompoundTag tag = new CompoundTag();
            tag.put(BLOCK_POS_KEY, NbtUtils.writeBlockPos(new BlockPos(7 + LAP_BLOCKS, 102, 0)));

            CompoundTag seated = table.seatedIn(new HomeLap(), HungSubject.class, tag);

            assertTrue(table.carriesPositions(HungSubject.class));
            assertEquals(new BlockPos(7, 102, 0), NbtUtils.readBlockPos(seated, BLOCK_POS_KEY).orElseThrow());
        }

        @Test
        void anIntTripleRegisteredOnASuperclassSeatsTheSubclassTag() {
            TagPositions.Table table = new TagPositions.Table();
            table.register(Hung.class, TagPositions.PositionShape.BLOCK_INT_TRIPLE,
                    TILE_X_KEY, TILE_Y_KEY, TILE_Z_KEY);

            CompoundTag tag = new CompoundTag();
            putTriple(tag, new BlockPos(11 + LAP_BLOCKS, 102, 0));

            CompoundTag seated = table.seatedIn(new HomeLap(), HungSubject.class, tag);

            assertEquals(new BlockPos(11, 102, 0), tripleIn(seated));
        }

        @Test
        void registeringATypeAgainKeepsTheKeysItAlreadyCarried() {
            TagPositions.Table table = new TagPositions.Table();
            table.register(PackedSubject.class, TagPositions.PositionShape.PACKED_LONG, PACKED_KEY);
            table.register(PackedSubject.class, TagPositions.PositionShape.VEC3_LIST, VEC3_KEY);

            CompoundTag tag = new CompoundTag();
            tag.putLong(PACKED_KEY, new BlockPos(3 + LAP_BLOCKS, 102, 0).asLong());
            tag.put(VEC3_KEY, doubleList(0.5 + LAP_BLOCKS, 0.5, 0.5));

            CompoundTag seated = table.seatedIn(new HomeLap(), PackedSubject.class, tag);

            assertEquals(new BlockPos(3, 102, 0), BlockPos.of(seated.getLong(PACKED_KEY)));
            assertEquals(new Vec3(0.5, 0.5, 0.5), vec3In(seated, VEC3_KEY));
        }

        @Test
        void aRegistrationMadeAfterAFirstReadReachesTheNextOne() {
            TagPositions.Table table = new TagPositions.Table();
            table.register(PackedSubject.class, TagPositions.PositionShape.PACKED_LONG, PACKED_KEY);

            CompoundTag read = new CompoundTag();
            read.put(VEC3_KEY, doubleList(0.5 + LAP_BLOCKS, 0.5, 0.5));
            assertSame(read, table.seatedIn(new HomeLap(), PackedSubject.class, read));

            table.register(PackedSubject.class, TagPositions.PositionShape.VEC3_LIST, VEC3_KEY);
            CompoundTag seated = table.seatedIn(new HomeLap(), PackedSubject.class, read);

            assertEquals(new Vec3(0.5, 0.5, 0.5), vec3In(seated, VEC3_KEY));
        }

        @Test
        void aTypeNobodyRegisteredCarriesNothingAndGetsItsTagBack() {
            TagPositions.Table table = new TagPositions.Table();
            table.register(PackedSubject.class, TagPositions.PositionShape.PACKED_LONG, PACKED_KEY);

            CompoundTag tag = new CompoundTag();
            tag.putLong(PACKED_KEY, new BlockPos(3 + LAP_BLOCKS, 102, 0).asLong());

            assertTrue(table.carriesPositions(PackedSubject.class));
            assertFalse(table.carriesPositions(UnregisteredSubject.class));
            assertSame(tag, table.seatedIn(new HomeLap(), UnregisteredSubject.class, tag));
        }

        @Test
        void aKeyCountThatDoesNotFillTheShapeIsRefused() {
            TagPositions.Table table = new TagPositions.Table();

            assertThrows(IllegalArgumentException.class, () -> table.register(
                    PackedSubject.class, TagPositions.PositionShape.BLOCK_INT_TRIPLE, TILE_X_KEY, TILE_Y_KEY));
            assertThrows(IllegalArgumentException.class, () -> table.register(
                    PackedSubject.class, TagPositions.PositionShape.PACKED_LONG));
        }

        @Test
        void anAddressedRegistrationSeatsInsideTheContainerItNames() {
            TagPositions.Table table = new TagPositions.Table();
            table.registerIn(PackedSubject.class, COMPOUND_KEY, TagPositions.PositionShape.BLOCK_POS, BLOCK_POS_KEY);
            table.registerInEach(PackedSubject.class, LIST_KEY, TagPositions.PositionShape.BLOCK_POS, BLOCK_POS_KEY);

            ListTag launched = new ListTag();
            launched.add(blockPosAt(new BlockPos(9 + LAP_BLOCKS, 102, 0)));
            CompoundTag tag = new CompoundTag();
            tag.put(COMPOUND_KEY, blockPosAt(new BlockPos(7 + LAP_BLOCKS, 102, 0)));
            tag.put(LIST_KEY, launched);

            CompoundTag seated = table.seatedIn(new HomeLap(), PackedSubject.class, tag);

            assertEquals(new BlockPos(7, 102, 0),
                    NbtUtils.readBlockPos(seated.getCompound(COMPOUND_KEY), BLOCK_POS_KEY).orElseThrow());
            assertEquals(new BlockPos(9, 102, 0), NbtUtils
                    .readBlockPos(seated.getList(LIST_KEY, Tag.TAG_COMPOUND).getCompound(0), BLOCK_POS_KEY)
                    .orElseThrow());
        }

        @Test
        void oneTableKnowsNothingOfWhatAnotherWasGiven() {
            TagPositions.Table registeredInto = new TagPositions.Table();
            TagPositions.Table untouched = new TagPositions.Table();

            registeredInto.register(PackedSubject.class, TagPositions.PositionShape.PACKED_LONG, PACKED_KEY);

            assertTrue(registeredInto.carriesPositions(PackedSubject.class));
            assertFalse(untouched.carriesPositions(PackedSubject.class));
        }
    }
}
