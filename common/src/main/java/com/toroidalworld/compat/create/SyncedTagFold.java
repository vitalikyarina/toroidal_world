package com.toroidalworld.compat.create;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public final class SyncedTagFold {
    private static final int VEC3_COMPONENTS = 3;

    private static final Map<Class<?>, List<TagPosition>> REGISTERED = new ConcurrentHashMap<>();
    private static final Map<Class<?>, List<TagPosition>> RESOLVED = new ConcurrentHashMap<>();

    public enum PositionShape {
        BLOCK_POS {
            @Override
            @Nullable Tag seated(WorldFold fold, BlockPos worldPosition, CompoundTag tag, String key) {
                BlockPos stored = NbtUtils.readBlockPos(tag, key).orElse(null);
                if (stored == null) {
                    return null;
                }

                BlockPos seated = fold.nearestCopy(worldPosition, stored);
                return seated.equals(stored) ? null : NbtUtils.writeBlockPos(seated);
            }
        },
        PACKED_LONG {
            @Override
            @Nullable Tag seated(WorldFold fold, BlockPos worldPosition, CompoundTag tag, String key) {
                if (!tag.contains(key, Tag.TAG_LONG)) {
                    return null;
                }

                BlockPos stored = BlockPos.of(tag.getLong(key));
                BlockPos seated = fold.nearestCopy(worldPosition, stored);
                return seated.equals(stored) ? null : LongTag.valueOf(seated.asLong());
            }
        },
        VEC3_LIST {
            @Override
            @Nullable Tag seated(WorldFold fold, BlockPos worldPosition, CompoundTag tag, String key) {
                ListTag list = tag.getList(key, Tag.TAG_DOUBLE);
                if (list.size() != VEC3_COMPONENTS) {
                    return null;
                }

                Vec3 stored = new Vec3(list.getDouble(0), list.getDouble(1), list.getDouble(2));
                Vec3 seated = fold.nearestCopy(Vec3.atCenterOf(worldPosition), stored);
                return seated.equals(stored) ? null : doubleList(seated);
            }
        };

        abstract @Nullable Tag seated(WorldFold fold, BlockPos worldPosition, CompoundTag tag, String key);
    }

    private record TagPosition(String key, PositionShape shape) {
    }

    public static void register(Class<?> blockEntityType, PositionShape shape, String... keys) {
        List<TagPosition> positions = REGISTERED.computeIfAbsent(blockEntityType, type -> new ArrayList<>());
        for (String key : keys) {
            positions.add(new TagPosition(key, shape));
        }

        RESOLVED.clear();
    }

    public static CompoundTag inFrameOf(BlockEntity blockEntity, CompoundTag tag) {
        Level level = blockEntity.getLevel();
        if (level == null || !level.isClientSide) {
            return tag;
        }

        WorldFold clientTransformer = WorldLoopAttachments.wrappedClientBoundsTransformerOf(level);
        return clientTransformer == null
                ? tag
                : seatedIn(clientTransformer, blockEntity.getBlockPos(), blockEntity.getClass(), tag);
    }

    static CompoundTag seatedIn(WorldFold fold, BlockPos worldPosition, Class<?> blockEntityType, CompoundTag tag) {
        List<TagPosition> positions = positionsOf(blockEntityType);
        if (positions.isEmpty()) {
            return tag;
        }

        Map<String, Tag> moved = null;
        for (TagPosition position : positions) {
            Tag seated = position.shape().seated(fold, worldPosition, tag, position.key());
            if (seated != null) {
                if (moved == null) {
                    moved = new HashMap<>();
                }

                moved.put(position.key(), seated);
            }
        }

        if (moved == null) {
            return tag;
        }

        CompoundTag folded = new CompoundTag();
        for (String key : tag.getAllKeys()) {
            folded.put(key, tag.get(key));
        }

        moved.forEach(folded::put);
        return folded;
    }

    private static List<TagPosition> positionsOf(Class<?> blockEntityType) {
        return RESOLVED.computeIfAbsent(blockEntityType, type -> {
            List<TagPosition> positions = new ArrayList<>();
            REGISTERED.forEach((registered, registeredPositions) -> {
                if (registered.isAssignableFrom(type)) {
                    positions.addAll(registeredPositions);
                }
            });

            return List.copyOf(positions);
        });
    }

    private static ListTag doubleList(Vec3 position) {
        ListTag list = new ListTag();
        list.add(DoubleTag.valueOf(position.x));
        list.add(DoubleTag.valueOf(position.y));
        list.add(DoubleTag.valueOf(position.z));
        return list;
    }

    private SyncedTagFold() {
    }
}
