package com.toroidalworld.player;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.toroidalworld.core.WorldFold;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class SeamTravel {
    public record Lap(double x, double z) {
        public static final Lap ZERO = new Lap(0.0, 0.0);

        static final Codec<Lap> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.DOUBLE.fieldOf("x").forGetter(Lap::x),
                Codec.DOUBLE.fieldOf("z").forGetter(Lap::z))
                .apply(instance, Lap::new));

        public double on(Direction.Axis axis) {
            return axis == Direction.Axis.X ? x : z;
        }

        Lap with(Direction.Axis axis, double travelled) {
            return axis == Direction.Axis.X ? new Lap(travelled, z) : new Lap(x, travelled);
        }
    }

    public record Step(Vec3 raw, Vec3 folded, Set<Direction.Axis> closed) {
        public boolean moved() {
            return folded.lengthSqr() > 0.0;
        }
    }

    public static final Codec<SeamTravel> CODEC = Codec.unboundedMap(Level.RESOURCE_KEY_CODEC, Lap.CODEC)
            .xmap(SeamTravel::new, travel -> travel.laps);

    private static final List<Direction.Axis> HORIZONTAL = List.of(Direction.Axis.X, Direction.Axis.Z);

    private final Map<ResourceKey<Level>, Lap> laps;

    private @Nullable ResourceKey<Level> lastSpace;

    private @Nullable Vec3 lastPosition;

    public SeamTravel() {
        this(Map.of());
    }

    private SeamTravel(Map<ResourceKey<Level>, Lap> laps) {
        this.laps = new HashMap<>(laps);
    }

    public Lap in(ResourceKey<Level> space) {
        return laps.getOrDefault(space, Lap.ZERO);
    }

    public void copyFrom(SeamTravel other) {
        this.laps.clear();
        this.laps.putAll(other.laps);
    }

    public Step advance(@Nullable WorldFold fold, ResourceKey<Level> space, Vec3 position) {
        ResourceKey<Level> previousSpace = this.lastSpace;
        Vec3 previousPosition = this.lastPosition;
        this.lastSpace = space;
        this.lastPosition = position;

        if (fold == null || !fold.decomposesPerAxis() || previousPosition == null || !space.equals(previousSpace)) {
            return new Step(Vec3.ZERO, Vec3.ZERO, Set.of());
        }

        Vec3 raw = position.subtract(previousPosition);
        Vec3 folded = fold.foldDelta(previousPosition, position);
        Set<Direction.Axis> closed = EnumSet.noneOf(Direction.Axis.class);
        Lap after = in(space);

        for (Direction.Axis axis : HORIZONTAL) {
            if (!fold.bounds().loops(axis)) {
                continue;
            }

            int width = fold.blockDomain(axis).domainLength;
            double travelled = after.on(axis) + (axis == Direction.Axis.X ? folded.x : folded.z);
            if (Math.abs(travelled) >= width) {
                travelled -= Math.signum(travelled) * width;
                closed.add(axis);
            }

            after = after.with(axis, travelled);
        }

        laps.put(space, after);
        return new Step(raw, folded, closed);
    }
}
