package com.toroidalworld.shape;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record FlatShape(WorldLoopBounds bounds, int skewChunks, @Nullable Mirror mirror) {
    private static final String SKEW_CHUNKS_KEY = "skew_chunks";
    private static final String MIRROR_KEY = "mirror";

    public static final int NO_SKEW = 0;

    public record Mirror(Direction.Axis axis, int lineChunk) {
        private static final String AXIS_KEY = "axis";
        private static final String LINE_CHUNK_KEY = "line_chunk";

        private static final String MIRRORS_Y_ERROR = "A flat identification never mirrors Y";

        private static final int AXIS_X_ID = 0;
        private static final int AXIS_Z_ID = 1;

        private static final Codec<Direction.Axis> AXIS_CODEC = Direction.Axis.CODEC.validate(
                axis -> axis == Direction.Axis.Y
                        ? DataResult.error(() -> MIRRORS_Y_ERROR)
                        : DataResult.success(axis));

        public static final Codec<Mirror> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        AXIS_CODEC.fieldOf(AXIS_KEY).forGetter(Mirror::axis),
                        Codec.INT.fieldOf(LINE_CHUNK_KEY).forGetter(Mirror::lineChunk)
                ).apply(instance, instance.stable(Mirror::new)));

        public static final StreamCodec<ByteBuf, Mirror> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.idMapper(Mirror::axisById, Mirror::idOfAxis), Mirror::axis,
                ByteBufCodecs.VAR_INT, Mirror::lineChunk,
                Mirror::new);

        public Mirror {
            if (axis == Direction.Axis.Y) {
                throw new IllegalArgumentException(MIRRORS_Y_ERROR);
            }
        }

        private static Direction.Axis axisById(int id) {
            return switch (id) {
                case AXIS_X_ID -> Direction.Axis.X;
                case AXIS_Z_ID -> Direction.Axis.Z;
                default -> throw new DecoderException("Not a mirrorable axis id: " + id);
            };
        }

        private static int idOfAxis(Direction.Axis axis) {
            return axis == Direction.Axis.X ? AXIS_X_ID : AXIS_Z_ID;
        }
    }

    public static final Codec<FlatShape> CODEC = RecordCodecBuilder.<Raw>create(
            instance -> instance.group(
                    WorldLoopBounds.MAP_CODEC.forGetter(Raw::bounds),
                    Codec.INT.optionalFieldOf(SKEW_CHUNKS_KEY, NO_SKEW).forGetter(Raw::skewChunks),
                    Mirror.CODEC.optionalFieldOf(MIRROR_KEY).forGetter(Raw::mirror)
            ).apply(instance, Raw::new))
            .flatXmap(Raw::toShape, FlatShape::toRaw);

    public static final StreamCodec<ByteBuf, FlatShape> STREAM_CODEC = StreamCodec.composite(
            WorldLoopBounds.STREAM_CODEC, FlatShape::bounds,
            ByteBufCodecs.VAR_INT, FlatShape::skewChunks,
            ByteBufCodecs.optional(Mirror.STREAM_CODEC), FlatShape::optionalMirror,
            (bounds, skewChunks, mirror) -> new FlatShape(bounds, skewChunks, mirror.orElse(null)));

    private record Raw(WorldLoopBounds bounds, int skewChunks, Optional<Mirror> mirror) {
        private DataResult<FlatShape> toShape() {
            try {
                return DataResult.success(new FlatShape(bounds, skewChunks, mirror.orElse(null)));
            } catch (IllegalArgumentException rejected) {
                return DataResult.error(rejected::getMessage);
            }
        }
    }

    public enum Identification {
        RECTANGLE,
        CYLINDER,
        MOBIUS,
        KLEIN,
        LATTICE_TORUS
    }

    public FlatShape {
        boolean xLooped = bounds.x() instanceof AxisBounds.Looped;
        boolean zLooped = bounds.z() instanceof AxisBounds.Looped;

        if (skewChunks != 0 && !(xLooped && zLooped)) {
            throw new IllegalArgumentException("A skewed lattice needs both axes looped, got " + bounds);
        }

        if (mirror != null) {
            if (skewChunks != 0) {
                throw new IllegalArgumentException("A skewed mirror is not one of the five flat identifications");
            }

            boolean glideAxisLooped = mirror.axis() == Direction.Axis.X ? zLooped : xLooped;
            if (!glideAxisLooped) {
                throw new IllegalArgumentException("A mirror on " + mirror.axis()
                        + " needs the axis it glides along to loop, got " + bounds);
            }
        }

        if (skewChunks != 0) {
            skewChunks = normalizeSkew(skewChunks, ((AxisBounds.Looped) bounds.x()).chunkWidth());
        }
    }

    public static FlatShape rectangle() {
        return new FlatShape(WorldLoopBounds.UNBOUNDED, 0, null);
    }

    public static FlatShape cylinder(WorldLoopBounds bounds) {
        return new FlatShape(bounds, 0, null);
    }

    public static FlatShape latticeTorus(WorldLoopBounds bounds, int skewChunks) {
        return new FlatShape(bounds, skewChunks, null);
    }

    public static FlatShape mirrored(WorldLoopBounds bounds, Direction.Axis mirroredAxis, int mirrorLineChunk) {
        return new FlatShape(bounds, 0, new Mirror(mirroredAxis, mirrorLineChunk));
    }

    public Identification identification() {
        boolean xLooped = bounds.x() instanceof AxisBounds.Looped;
        boolean zLooped = bounds.z() instanceof AxisBounds.Looped;
        if (!xLooped && !zLooped) {
            return Identification.RECTANGLE;
        }

        if (xLooped && zLooped) {
            return mirror != null ? Identification.KLEIN : Identification.LATTICE_TORUS;
        }

        return mirror != null ? Identification.MOBIUS : Identification.CYLINDER;
    }

    public boolean isMirrored() {
        return mirror != null;
    }

    public boolean decomposesPerAxis() {
        return mirror == null && skewChunks == 0;
    }

    public boolean preservesLocalIndices() {
        return mirror == null;
    }

    private Optional<Mirror> optionalMirror() {
        return Optional.ofNullable(mirror);
    }

    private DataResult<Raw> toRaw() {
        return DataResult.success(new Raw(bounds, skewChunks, optionalMirror()));
    }

    private static int normalizeSkew(int skewChunks, int xChunkWidth) {
        int remainder = Math.floorMod(skewChunks, xChunkWidth);
        return 2 * remainder > xChunkWidth ? remainder - xChunkWidth : remainder;
    }
}
