package com.toroidalworld.net;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;

public final class TagPositions {
    private static final int VEC3_COMPONENTS = 3;
    private static final int BLOCK_TRIPLE_KEYS = 3;

    private static final int TRIPLE_X = 0;
    private static final int TRIPLE_Y = 1;
    private static final int TRIPLE_Z = 2;

    public interface Seat {
        BlockPos seat(BlockPos stored);

        Vec3 seat(Vec3 stored);
    }

    public enum PositionShape {
        BLOCK_POS(1) {
            @Override
            @Nullable CompoundTag seated(Seat seat, CompoundTag tag, List<String> keys) {
                String key = keys.getFirst();
                BlockPos stored = NbtUtils.readBlockPos(tag, key).orElse(null);
                if (stored == null) {
                    return null;
                }

                BlockPos seated = seat.seat(stored);
                return seated.equals(stored) ? null : fragment(key, NbtUtils.writeBlockPos(seated));
            }
        },
        PACKED_LONG(1) {
            @Override
            @Nullable CompoundTag seated(Seat seat, CompoundTag tag, List<String> keys) {
                String key = keys.getFirst();
                if (!tag.contains(key, Tag.TAG_LONG)) {
                    return null;
                }

                BlockPos stored = BlockPos.of(tag.getLong(key));
                BlockPos seated = seat.seat(stored);
                return seated.equals(stored) ? null : fragment(key, LongTag.valueOf(seated.asLong()));
            }
        },
        VEC3_LIST(1) {
            @Override
            @Nullable CompoundTag seated(Seat seat, CompoundTag tag, List<String> keys) {
                String key = keys.getFirst();
                ListTag list = tag.getList(key, Tag.TAG_DOUBLE);
                if (list.size() != VEC3_COMPONENTS) {
                    return null;
                }

                Vec3 stored = new Vec3(list.getDouble(0), list.getDouble(1), list.getDouble(2));
                Vec3 seated = seat.seat(stored);
                return seated.equals(stored) ? null : fragment(key, doubleList(seated));
            }
        },
        BLOCK_INT_TRIPLE(BLOCK_TRIPLE_KEYS) {
            @Override
            @Nullable CompoundTag seated(Seat seat, CompoundTag tag, List<String> keys) {
                for (String key : keys) {
                    if (!tag.contains(key, Tag.TAG_INT)) {
                        return null;
                    }
                }

                BlockPos stored = new BlockPos(tag.getInt(keys.get(TRIPLE_X)), tag.getInt(keys.get(TRIPLE_Y)),
                        tag.getInt(keys.get(TRIPLE_Z)));
                BlockPos seated = seat.seat(stored);
                if (seated.equals(stored)) {
                    return null;
                }

                CompoundTag moved = new CompoundTag();
                moved.putInt(keys.get(TRIPLE_X), seated.getX());
                moved.putInt(keys.get(TRIPLE_Y), seated.getY());
                moved.putInt(keys.get(TRIPLE_Z), seated.getZ());
                return moved;
            }
        };

        private final int keyCount;

        PositionShape(int keyCount) {
            this.keyCount = keyCount;
        }

        public int keyCount() {
            return keyCount;
        }

        abstract @Nullable CompoundTag seated(Seat seat, CompoundTag tag, List<String> keys);
    }

    public record TagPosition(List<String> keys, PositionShape shape) {
        public TagPosition {
            keys = List.copyOf(keys);
            if (keys.size() != shape.keyCount()) {
                throw new IllegalArgumentException(shape + " spreads a position over " + shape.keyCount()
                        + " keys, and " + keys.size() + " were given");
            }
        }
    }

    public static final class Table {
        private final Map<Class<?>, List<TagPosition>> registered = new ConcurrentHashMap<>();
        private volatile Map<Class<?>, List<TagPosition>> resolved = new ConcurrentHashMap<>();

        public void register(Class<?> subjectType, PositionShape shape, String... keys) {
            int keyCount = shape.keyCount();
            if (keys.length == 0 || keys.length % keyCount != 0) {
                throw new IllegalArgumentException(shape + " spreads a position over " + keyCount + " keys, and "
                        + keys.length + " were registered on " + subjectType.getName());
            }

            List<TagPosition> added = new ArrayList<>();
            for (int index = 0; index < keys.length; index += keyCount) {
                added.add(new TagPosition(Arrays.asList(keys).subList(index, index + keyCount), shape));
            }

            registered.merge(subjectType, List.copyOf(added), Table::joined);
            resolved = new ConcurrentHashMap<>();
        }

        public boolean carriesPositions(Class<?> subjectType) {
            return !positionsOf(subjectType).isEmpty();
        }

        public CompoundTag seatedIn(Seat seat, Class<?> subjectType, CompoundTag tag) {
            List<TagPosition> positions = positionsOf(subjectType);
            return positions.isEmpty() ? tag : TagPositions.seatedIn(seat, positions, tag);
        }

        private List<TagPosition> positionsOf(Class<?> subjectType) {
            return resolved.computeIfAbsent(subjectType, type -> {
                List<TagPosition> positions = new ArrayList<>();
                registered.forEach((registeredType, registeredPositions) -> {
                    if (registeredType.isAssignableFrom(type)) {
                        positions.addAll(registeredPositions);
                    }
                });

                return List.copyOf(positions);
            });
        }

        private static List<TagPosition> joined(List<TagPosition> existing, List<TagPosition> added) {
            List<TagPosition> all = new ArrayList<>(existing);
            all.addAll(added);
            return List.copyOf(all);
        }
    }

    public static CompoundTag seatedIn(Seat seat, List<TagPosition> positions, CompoundTag tag) {
        CompoundTag folded = null;
        for (TagPosition position : positions) {
            CompoundTag moved = position.shape().seated(seat, tag, position.keys());
            if (moved == null) {
                continue;
            }

            if (folded == null) {
                folded = new CompoundTag();
                for (String key : tag.getAllKeys()) {
                    folded.put(key, tag.get(key));
                }
            }

            for (String key : moved.getAllKeys()) {
                folded.put(key, moved.get(key));
            }
        }

        return folded == null ? tag : folded;
    }

    private static CompoundTag fragment(String key, Tag value) {
        CompoundTag moved = new CompoundTag();
        moved.put(key, value);
        return moved;
    }

    private static ListTag doubleList(Vec3 position) {
        ListTag list = new ListTag();
        list.add(DoubleTag.valueOf(position.x));
        list.add(DoubleTag.valueOf(position.y));
        list.add(DoubleTag.valueOf(position.z));
        return list;
    }

    private TagPositions() {
    }
}
