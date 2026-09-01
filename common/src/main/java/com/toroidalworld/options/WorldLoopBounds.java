package com.toroidalworld.options;

import java.util.Optional;

import com.toroidalworld.core.CoordinateConstants;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.network.VarInt;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.border.WorldBorder;

public record WorldLoopBounds(AxisBounds x, AxisBounds z) {

    public sealed interface AxisBounds {
        String MIN_CHUNK_KEY = "min_chunk";
        String MAX_CHUNK_KEY = "max_chunk";

        record Looped(int minChunk, int maxChunk) implements AxisBounds {
            public static Looped ofWidth(int chunkWidth) {
                int minChunk = -(chunkWidth / 2);
                return new Looped(minChunk, chunkWidth + minChunk);
            }

            public int chunkWidth() {
                return maxChunk - minChunk;
            }

            public int minBlock() {
                return minChunk * CoordinateConstants.CHUNK_WIDTH;
            }

            public int maxBlock() {
                return maxChunk * CoordinateConstants.CHUNK_WIDTH;
            }

            public int blockWidth() {
                return chunkWidth() * CoordinateConstants.CHUNK_WIDTH;
            }

            @Override
            public boolean isOver(double blockCoord) {
                return blockCoord < minBlock() || blockCoord >= maxBlock();
            }

            @Override
            public boolean fitsInHalf(double blockSpan) {
                return 2 * blockSpan <= blockWidth();
            }

            @Override
            public boolean coversWorld(double blockSpan) {
                return blockSpan >= blockWidth();
            }

            @Override
            public boolean foldsOntoItself(int chunkCount) {
                return chunkCount > chunkWidth();
            }

            @Override
            public int maxViewDistance() {
                return Math.max(1, chunkWidth() / 2 - CoordinateConstants.VIEW_DISTANCE_MARGIN);
            }
        }

        record Unbounded() implements AxisBounds {
            public static final Unbounded INSTANCE = new Unbounded();

            @Override
            public boolean isOver(double blockCoord) {
                return false;
            }

            @Override
            public boolean fitsInHalf(double blockSpan) {
                return true;
            }

            @Override
            public boolean coversWorld(double blockSpan) {
                return false;
            }

            @Override
            public boolean foldsOntoItself(int chunkCount) {
                return false;
            }

            @Override
            public int maxViewDistance() {
                return Integer.MAX_VALUE;
            }
        }

        boolean isOver(double blockCoord);

        boolean fitsInHalf(double blockSpan);

        boolean coversWorld(double blockSpan);

        boolean foldsOntoItself(int chunkCount);

        int maxViewDistance();

        StreamCodec<ByteBuf, AxisBounds> STREAM_CODEC = StreamCodec.of(
                (buffer, axis) -> {
                    switch (axis) {
                        case Looped looped -> {
                            buffer.writeBoolean(true);
                            VarInt.write(buffer, looped.minChunk());
                            VarInt.write(buffer, looped.maxChunk());
                        }
                        case Unbounded() -> buffer.writeBoolean(false);
                    }
                },
                buffer -> buffer.readBoolean()
                        ? new Looped(VarInt.read(buffer), VarInt.read(buffer))
                        : Unbounded.INSTANCE);

        Codec<AxisBounds> CODEC = Codec.mapPair(
                        Codec.INT.optionalFieldOf(MIN_CHUNK_KEY),
                        Codec.INT.optionalFieldOf(MAX_CHUNK_KEY))
                .codec()
                .flatXmap(AxisBounds::read, AxisBounds::write);

        private static DataResult<AxisBounds> read(Pair<Optional<Integer>, Optional<Integer>> bounds) {
            Optional<Integer> minChunk = bounds.getFirst();
            Optional<Integer> maxChunk = bounds.getSecond();
            if (minChunk.isEmpty() && maxChunk.isEmpty()) {
                return DataResult.success(Unbounded.INSTANCE);
            }

            if (minChunk.isEmpty() || maxChunk.isEmpty()) {
                return DataResult.error(() -> "An axis needs both chunk bounds or neither, got only one of "
                        + MIN_CHUNK_KEY + "/" + MAX_CHUNK_KEY);
            }

            if (minChunk.get() == -LEGACY_DISABLED_AXIS_RADIUS || maxChunk.get() == LEGACY_DISABLED_AXIS_RADIUS) {
                return DataResult.success(Unbounded.INSTANCE);
            }

            if (minChunk.get() >= maxChunk.get()) {
                return DataResult.error(() -> "A looped axis needs " + MIN_CHUNK_KEY + " < " + MAX_CHUNK_KEY
                        + ", got [" + minChunk.get() + ", " + maxChunk.get() + ")");
            }

            return DataResult.success(new Looped(minChunk.get(), maxChunk.get()));
        }

        private static DataResult<Pair<Optional<Integer>, Optional<Integer>>> write(AxisBounds axis) {
            return DataResult.success(switch (axis) {
                case Looped looped -> Pair.of(Optional.of(looped.minChunk()), Optional.of(looped.maxChunk()));
                case Unbounded() -> Pair.of(Optional.empty(), Optional.empty());
            });
        }
    }

    // The frozen literal the retired sentinel model wrote for a non-looping axis; legacy save files still carry it.
    private static final int LEGACY_DISABLED_AXIS_RADIUS =
            (int) (WorldBorder.MAX_CENTER_COORDINATE / CoordinateConstants.CHUNK_WIDTH) + 8192;

    private static final String X_KEY = "x";
    private static final String Z_KEY = "z";

    private static final String NOT_A_HORIZONTAL_AXIS = "Not a horizontal axis: ";

    public static final MapCodec<WorldLoopBounds> MAP_CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    AxisBounds.CODEC.fieldOf(X_KEY).forGetter(WorldLoopBounds::x),
                    AxisBounds.CODEC.fieldOf(Z_KEY).forGetter(WorldLoopBounds::z)
            ).apply(instance, instance.stable(WorldLoopBounds::new)));

    public static final Codec<WorldLoopBounds> CODEC = MAP_CODEC.codec();

    public static final StreamCodec<ByteBuf, WorldLoopBounds> STREAM_CODEC = StreamCodec.composite(
            AxisBounds.STREAM_CODEC, WorldLoopBounds::x,
            AxisBounds.STREAM_CODEC, WorldLoopBounds::z,
            WorldLoopBounds::new);

    public static final WorldLoopBounds UNBOUNDED =
            new WorldLoopBounds(AxisBounds.Unbounded.INSTANCE, AxisBounds.Unbounded.INSTANCE);

    public WorldLoopBounds(int xMinChunk, int xMaxChunk, int zMinChunk, int zMaxChunk) {
        this(new AxisBounds.Looped(xMinChunk, xMaxChunk), new AxisBounds.Looped(zMinChunk, zMaxChunk));
    }

    public static WorldLoopBounds ofWidth(int chunkWidth) {
        AxisBounds.Looped looped = AxisBounds.Looped.ofWidth(chunkWidth);
        return new WorldLoopBounds(looped, looped);
    }

    public static WorldLoopBounds ofWidth(Direction.Axis axis, int chunkWidth) {
        AxisBounds.Looped looped = AxisBounds.Looped.ofWidth(chunkWidth);
        return switch (axis) {
            case X -> new WorldLoopBounds(looped, AxisBounds.Unbounded.INSTANCE);
            case Z -> new WorldLoopBounds(AxisBounds.Unbounded.INSTANCE, looped);
            case Y -> throw new IllegalArgumentException(NOT_A_HORIZONTAL_AXIS + axis);
        };
    }

    public AxisBounds axis(Direction.Axis axis) {
        return switch (axis) {
            case X -> x;
            case Z -> z;
            case Y -> throw new IllegalArgumentException(NOT_A_HORIZONTAL_AXIS + axis);
        };
    }

    public boolean loops(Direction.Axis axis) {
        return axis(axis) instanceof AxisBounds.Looped;
    }

    public int chunkWidth(Direction.Axis axis) {
        return switch (axis(axis)) {
            case AxisBounds.Looped looped -> looped.chunkWidth();
            case AxisBounds.Unbounded() -> throw new IllegalStateException("chunkWidth() of the unbounded axis " + axis);
        };
    }

    public WorldLoopBounds scaledDown(int scale) {
        return new WorldLoopBounds(scaledDown(x, scale), scaledDown(z, scale));
    }

    private static AxisBounds scaledDown(AxisBounds axis, int scale) {
        return switch (axis) {
            case AxisBounds.Looped looped -> AxisBounds.Looped.ofWidth(looped.chunkWidth() / scale);
            case AxisBounds.Unbounded() -> axis;
        };
    }

    public boolean isSquare() {
        return x instanceof AxisBounds.Looped xLooped
                && z instanceof AxisBounds.Looped zLooped
                && xLooped.chunkWidth() == zLooped.chunkWidth()
                && xLooped.chunkWidth() > 0;
    }

    public int chunkWidth() {
        return switch (x) {
            case AxisBounds.Looped looped -> looped.chunkWidth();
            case AxisBounds.Unbounded() -> throw new IllegalStateException("chunkWidth() of an unbounded axis");
        };
    }

    public int maxViewDistance() {
        return Math.min(x.maxViewDistance(), z.maxViewDistance());
    }
}
