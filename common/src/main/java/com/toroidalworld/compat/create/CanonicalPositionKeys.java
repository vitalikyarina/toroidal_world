package com.toroidalworld.compat.create;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;

public final class CanonicalPositionKeys {
    public static Set<BlockPos> set(@Nullable BlockGetter reader) {
        WorldLoopTransformer transformer = transformerOf(reader);
        return transformer == null ? new HashSet<>() : new CanonicalSet(transformer);
    }

    public static <V> Map<BlockPos, V> map(@Nullable BlockGetter reader) {
        WorldLoopTransformer transformer = transformerOf(reader);
        return transformer == null ? new HashMap<>() : new CanonicalMap<>(transformer);
    }

    private static @Nullable WorldLoopTransformer transformerOf(@Nullable BlockGetter reader) {
        if (!(reader instanceof LevelReader levelReader)) {
            return null;
        }

        Level level = WorldLoopAttachments.levelOf(levelReader);
        return level == null ? null : WorldLoopAttachments.wrappedTransformerOf(level);
    }

    private static Object key(WorldLoopTransformer transformer, Object candidate) {
        return candidate instanceof BlockPos pos ? transformer.blocks.wrap(pos) : candidate;
    }

    private static final class CanonicalSet extends AbstractSet<BlockPos> {
        private final WorldLoopTransformer transformer;
        private final Set<BlockPos> keys = new HashSet<>();

        private CanonicalSet(WorldLoopTransformer transformer) {
            this.transformer = transformer;
        }

        @Override
        public boolean add(BlockPos pos) {
            return keys.add(transformer.blocks.wrap(pos));
        }

        @Override
        public boolean contains(Object candidate) {
            return keys.contains(key(transformer, candidate));
        }

        @Override
        public boolean remove(Object candidate) {
            return keys.remove(key(transformer, candidate));
        }

        @Override
        public Iterator<BlockPos> iterator() {
            return keys.iterator();
        }

        @Override
        public int size() {
            return keys.size();
        }

        @Override
        public void clear() {
            keys.clear();
        }
    }

    private static final class CanonicalMap<V> extends AbstractMap<BlockPos, V> {
        private final WorldLoopTransformer transformer;
        private final Map<BlockPos, V> entries = new HashMap<>();

        private CanonicalMap(WorldLoopTransformer transformer) {
            this.transformer = transformer;
        }

        @Override
        public @Nullable V put(BlockPos pos, V value) {
            return entries.put(transformer.blocks.wrap(pos), value);
        }

        @Override
        public @Nullable V get(Object candidate) {
            return entries.get(key(transformer, candidate));
        }

        @Override
        public boolean containsKey(Object candidate) {
            return entries.containsKey(key(transformer, candidate));
        }

        @Override
        public @Nullable V remove(Object candidate) {
            return entries.remove(key(transformer, candidate));
        }

        @Override
        public Set<Entry<BlockPos, V>> entrySet() {
            return entries.entrySet();
        }

        @Override
        public void clear() {
            entries.clear();
        }
    }

    private CanonicalPositionKeys() {
    }
}
