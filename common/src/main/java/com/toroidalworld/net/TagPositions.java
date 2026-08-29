package com.toroidalworld.net;

import java.util.ArrayList;
import java.util.HashMap;
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

    public interface Seat {
        BlockPos seat(BlockPos stored);

        Vec3 seat(Vec3 stored);
    }

    public enum PositionShape {
        BLOCK_POS {
            @Override
            @Nullable Tag seated(Seat seat, CompoundTag tag, String key) {
                BlockPos stored = NbtUtils.readBlockPos(tag, key).orElse(null);
                if (stored == null) {
                    return null;
                }

                BlockPos seated = seat.seat(stored);
                return seated.equals(stored) ? null : NbtUtils.writeBlockPos(seated);
            }
        },
        PACKED_LONG {
            @Override
            @Nullable Tag seated(Seat seat, CompoundTag tag, String key) {
                if (!tag.contains(key, Tag.TAG_LONG)) {
                    return null;
                }

                BlockPos stored = BlockPos.of(tag.getLong(key));
                BlockPos seated = seat.seat(stored);
                return seated.equals(stored) ? null : LongTag.valueOf(seated.asLong());
            }
        },
        VEC3_LIST {
            @Override
            @Nullable Tag seated(Seat seat, CompoundTag tag, String key) {
                ListTag list = tag.getList(key, Tag.TAG_DOUBLE);
                if (list.size() != VEC3_COMPONENTS) {
                    return null;
                }

                Vec3 stored = new Vec3(list.getDouble(0), list.getDouble(1), list.getDouble(2));
                Vec3 seated = seat.seat(stored);
                return seated.equals(stored) ? null : doubleList(seated);
            }
        };

        abstract @Nullable Tag seated(Seat seat, CompoundTag tag, String key);
    }

    public record TagPosition(String key, PositionShape shape) {
    }

    public static final class Table {
        private final Map<Class<?>, List<TagPosition>> registered = new ConcurrentHashMap<>();
        private final Map<Class<?>, List<TagPosition>> resolved = new ConcurrentHashMap<>();

        public void register(Class<?> subjectType, PositionShape shape, String... keys) {
            List<TagPosition> positions = registered.computeIfAbsent(subjectType, type -> new ArrayList<>());
            for (String key : keys) {
                positions.add(new TagPosition(key, shape));
            }

            resolved.clear();
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
    }

    public static CompoundTag seatedIn(Seat seat, List<TagPosition> positions, CompoundTag tag) {
        Map<String, Tag> moved = null;
        for (TagPosition position : positions) {
            Tag seated = position.shape().seated(seat, tag, position.key());
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
