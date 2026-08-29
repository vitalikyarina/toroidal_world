package com.toroidalworld.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;

class SpawnBufferFoldTest {
    private static final int LAP_BLOCKS = 512;

    private static final String POS_KEY = "Pos";
    private static final String ATTACHMENT_KEY = "ControllerPos";
    private static final String UNRELATED_KEY = "From";
    private static final String TILE_X_KEY = "TileX";
    private static final String TILE_Y_KEY = "TileY";
    private static final String TILE_Z_KEY = "TileZ";

    private static class SpawningEntity {
    }

    private static class HangingEntity {
    }

    private static final class HangingDiagramEntity extends HangingEntity {
    }

    private static final class QuaternionEntity {
    }

    private static final TagPositions.Seat HOME = new TagPositions.Seat() {
        @Override
        public BlockPos seat(BlockPos stored) {
            return new BlockPos(homeX(stored.getX()), stored.getY(), stored.getZ());
        }

        @Override
        public Vec3 seat(Vec3 stored) {
            return new Vec3(stored.x - Math.floorDiv((int) Math.floor(stored.x), LAP_BLOCKS) * LAP_BLOCKS,
                    stored.y, stored.z);
        }

        private static int homeX(int x) {
            return x - Math.floorDiv(x, LAP_BLOCKS) * LAP_BLOCKS;
        }
    };

    static {
        SpawnBufferFold.register(SpawningEntity.class, TagPositions.PositionShape.VEC3_LIST, POS_KEY);
        SpawnBufferFold.register(SpawningEntity.class, TagPositions.PositionShape.BLOCK_POS, ATTACHMENT_KEY);
        SpawnBufferFold.register(HangingEntity.class, TagPositions.PositionShape.BLOCK_POS, ATTACHMENT_KEY);
        SpawnBufferFold.register(HangingEntity.class, TagPositions.PositionShape.BLOCK_INT_TRIPLE,
                TILE_X_KEY, TILE_Y_KEY, TILE_Z_KEY);
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
    void aSubclassInheritsTheKeysRegisteredOnItsSuperclass() {
        CompoundTag tag = new CompoundTag();
        tag.put(ATTACHMENT_KEY, NbtUtils.writeBlockPos(new BlockPos(7 + LAP_BLOCKS, 102, 0)));

        CompoundTag seated = SpawnBufferFold.seatedIn(HOME, HangingDiagramEntity.class, tag);

        assertTrue(SpawnBufferFold.carriesPositions(HangingDiagramEntity.class));
        assertEquals(new BlockPos(7, 102, 0), NbtUtils.readBlockPos(seated, ATTACHMENT_KEY).orElseThrow());
    }

    @Test
    void aSubclassInheritsTheIntTripleRegisteredOnItsSuperclass() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(TILE_X_KEY, 11 + LAP_BLOCKS);
        tag.putInt(TILE_Y_KEY, 102);
        tag.putInt(TILE_Z_KEY, 0);

        CompoundTag seated = SpawnBufferFold.seatedIn(HOME, HangingDiagramEntity.class, tag);

        assertEquals(11, seated.getInt(TILE_X_KEY));
        assertEquals(102, seated.getInt(TILE_Y_KEY));
        assertEquals(0, seated.getInt(TILE_Z_KEY));
    }

    @Test
    void aRegisteredTypeIsAnnouncedAsCarryingPositions() {
        assertTrue(SpawnBufferFold.carriesPositions(SpawningEntity.class));
        assertFalse(SpawnBufferFold.carriesPositions(QuaternionEntity.class));
    }

    @Test
    void everyShapeRegisteredOnOneTypeIsSeatedInOnePass() {
        CompoundTag tag = new CompoundTag();
        tag.put(POS_KEY, doubleList(17.0 + LAP_BLOCKS, 102.0, 1.0));
        tag.put(ATTACHMENT_KEY, NbtUtils.writeBlockPos(new BlockPos(7 + LAP_BLOCKS, 102, 0)));

        CompoundTag seated = SpawnBufferFold.seatedIn(HOME, SpawningEntity.class, tag);

        assertEquals(new Vec3(17.0, 102.0, 1.0), vec3In(seated, POS_KEY));
        assertEquals(new BlockPos(7, 102, 0), NbtUtils.readBlockPos(seated, ATTACHMENT_KEY).orElseThrow());
    }

    @Test
    void theOffsetKeysBesideThePositionAreLeftAsTheyAre() {
        CompoundTag tag = new CompoundTag();
        tag.put(POS_KEY, doubleList(17.0 + LAP_BLOCKS, 102.0, 1.0));
        tag.put(UNRELATED_KEY, doubleList(-1.0, 0.0, -1.0));

        CompoundTag seated = SpawnBufferFold.seatedIn(HOME, SpawningEntity.class, tag);

        assertEquals(new Vec3(-1.0, 0.0, -1.0), vec3In(seated, UNRELATED_KEY));
    }

    @Test
    void anUnregisteredTypeIsTheArgumentBack() {
        CompoundTag tag = new CompoundTag();
        tag.put(POS_KEY, doubleList(17.0 + LAP_BLOCKS, 102.0, 1.0));

        assertSame(tag, SpawnBufferFold.seatedIn(HOME, QuaternionEntity.class, tag));
    }

    @Test
    void aBufferAlreadyHomeIsTheArgumentBack() {
        CompoundTag tag = new CompoundTag();
        tag.put(POS_KEY, doubleList(17.0, 102.0, 1.0));

        assertSame(tag, SpawnBufferFold.seatedIn(HOME, SpawningEntity.class, tag));
    }
}
