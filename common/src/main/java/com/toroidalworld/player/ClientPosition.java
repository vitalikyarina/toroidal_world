package com.toroidalworld.player;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.toroidalworld.core.LogRateGate;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.core.WrapDomain;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public final class ClientPosition {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Written on the server thread and read on the network thread, so the four values change together or not at all.
    private record Mirror(double x, double z, @Nullable ResourceKey<Level> space, WorldFold transformer) {
    }

    public record BorderCenter(double x, double z) {
    }

    private volatile Mirror mirror = new Mirror(0.0, 0.0, null, WorldFolds.NOOP);

    private volatile @Nullable BlockPos heldSpawn;

    private volatile @Nullable BorderCenter heldBorderCenter;

    // One record because the two coordinates and their space are one fact: written on the server thread, read on the network thread.
    private volatile @Nullable ChunkPos heldCacheCenter;

    private final LogRateGate warnGate = new LogRateGate();

    public double x() {
        return seededMirror().x();
    }

    public double z() {
        return seededMirror().z();
    }

    private Mirror seededMirror() {
        Mirror currMirror = this.mirror;
        if (currMirror.space() == null) {
            throw new IllegalStateException("ClientPosition mirror read before the first rebase seeded it");
        }
        return currMirror;
    }

    public void setX(double x, MirrorWriter writer) {
        Mirror currMirror = this.mirror;
        double seatedX = clientCopy(writer, Direction.Axis.X, currMirror, x);
        checkStep(writer, Direction.Axis.X, currMirror, seatedX);
        this.mirror = new Mirror(seatedX, currMirror.z(), currMirror.space(), currMirror.transformer());
    }

    public void setZ(double z, MirrorWriter writer) {
        Mirror currMirror = this.mirror;
        double seatedZ = clientCopy(writer, Direction.Axis.Z, currMirror, z);
        checkStep(writer, Direction.Axis.Z, currMirror, seatedZ);
        this.mirror = new Mirror(currMirror.x(), seatedZ, currMirror.space(), currMirror.transformer());
    }

    public void set(double x, double z, MirrorWriter writer) {
        Mirror currMirror = this.mirror;
        double seatedX = clientCopy(writer, Direction.Axis.X, currMirror, x);
        double seatedZ = clientCopy(writer, Direction.Axis.Z, currMirror, z);
        checkStep(writer, Direction.Axis.X, currMirror, seatedX);
        checkStep(writer, Direction.Axis.Z, currMirror, seatedZ);
        this.mirror = new Mirror(seatedX, seatedZ, currMirror.space(), currMirror.transformer());
    }

    public boolean describes(ResourceKey<Level> dimension) {
        return dimension.equals(this.mirror.space());
    }

    public void rebase(double x, double z, ResourceKey<Level> dimension, WorldFold transformer) {
        this.mirror = new Mirror(x, z, dimension, transformer);
        this.heldSpawn = null;
        this.heldBorderCenter = null;
        this.heldCacheCenter = null;
    }

    public @Nullable BlockPos heldSpawn() {
        return this.heldSpawn;
    }

    public void setHeldSpawn(BlockPos heldSpawn) {
        this.heldSpawn = heldSpawn;
    }

    public @Nullable BorderCenter heldBorderCenter() {
        return this.heldBorderCenter;
    }

    public void setHeldBorderCenter(BorderCenter heldBorderCenter) {
        this.heldBorderCenter = heldBorderCenter;
    }

    public @Nullable ChunkPos heldCacheCenter() {
        return this.heldCacheCenter;
    }

    public void setHeldCacheCenter(ChunkPos heldCacheCenter) {
        this.heldCacheCenter = heldCacheCenter;
    }

    public ChunkPos chunk() {
        Mirror currMirror = seededMirror();
        return new ChunkPos(
                SectionPos.blockToSectionCoord(currMirror.x()),
                SectionPos.blockToSectionCoord(currMirror.z()));
    }

    private static double clientCopy(MirrorWriter writer, Direction.Axis axis, Mirror currMirror, double reported) {
        if (!writer.clientAuthored()) {
            return reported;
        }

        double current = axis == Direction.Axis.X ? currMirror.x() : currMirror.z();
        return currMirror.transformer().blockDomain(axis).unwrapAround(current, reported);
    }

    private void checkStep(MirrorWriter writer, Direction.Axis axis, Mirror currMirror, double to) {
        WrapDomain domain = currMirror.transformer().blockDomain(axis);
        double from = axis == Direction.Axis.X ? currMirror.x() : currMirror.z();
        if (!domain.spansSeam(from, to) || !warnGate.tryPass()) {
            return;
        }

        LOGGER.warn("Half-world step invariant violated in {} by {}: mirror {} stepped from {} to {} without a rebase",
                spaceName(currMirror.space()), writer.key(), axis.getName(), from, to);
    }

    private static Object spaceName(@Nullable ResourceKey<Level> space) {
        return space == null ? "unseeded space" : space.location();
    }
}
