package com.toroidalworld.compat.create;

import java.util.AbstractSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldFold;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

public final class LappedKeys extends AbstractSet<BlockPos> {
    private final WorldFold fold;
    private final Map<BlockPos, BlockPos> claims = new HashMap<>();

    private @Nullable Lap lap;

    public record Lap(BlockPos arrived, BlockPos claimed, BlockPos key) {
    }

    public static Set<BlockPos> set(@Nullable BlockGetter reader) {
        WorldFold transformer = CanonicalPositionKeys.transformerOf(reader);
        return transformer == null ? new HashSet<>() : new LappedKeys(transformer);
    }

    LappedKeys(WorldFold fold) {
        this.fold = fold;
    }

    public @Nullable Lap lapped() {
        return lap;
    }

    @Override
    public boolean add(BlockPos pos) {
        BlockPos key = fold.fold(pos);
        BlockPos claimed = claims.putIfAbsent(key, pos);
        if (claimed == null) {
            return true;
        }

        witness(pos, claimed, key);
        return false;
    }

    @Override
    public boolean contains(Object candidate) {
        if (!(candidate instanceof BlockPos pos)) {
            return false;
        }

        BlockPos key = fold.fold(pos);
        BlockPos claimed = claims.get(key);
        if (claimed == null) {
            return false;
        }

        witness(pos, claimed, key);
        return true;
    }

    @Override
    public boolean remove(Object candidate) {
        return candidate instanceof BlockPos pos && claims.remove(fold.fold(pos)) != null;
    }

    @Override
    public Iterator<BlockPos> iterator() {
        return claims.values().iterator();
    }

    @Override
    public int size() {
        return claims.size();
    }

    @Override
    public void clear() {
        claims.clear();
    }

    private void witness(BlockPos arrived, BlockPos claimed, BlockPos key) {
        if (lap == null && !arrived.equals(claimed)) {
            lap = new Lap(arrived, claimed, key);
        }
    }
}
