package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.accessors.TransformerHolder;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.core.WrapDomain;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

@Mixin(WorldBorder.class)
public class WorldBorderMixin implements TransformerHolder {
    @Unique
    private WorldFold toroidal$transformer = WorldFolds.NOOP;

    @Unique
    private double @Nullable [] toroidal$wallBounds;

    @Unique
    private @Nullable VoxelShape toroidal$wall;

    @Shadow
    public double getMinX() {
        throw new AssertionError();
    }

    @Shadow
    public double getMaxX() {
        throw new AssertionError();
    }

    @Shadow
    public double getMinZ() {
        throw new AssertionError();
    }

    @Shadow
    public double getMaxZ() {
        throw new AssertionError();
    }

    @Override
    public WorldFold toroidal$transformer() {
        return this.toroidal$transformer;
    }

    @Override
    public void toroidal$setTransformer(WorldFold transformer) {
        this.toroidal$transformer = transformer;
    }

    @ModifyReturnValue(method = "isWithinBounds(DDD)Z", at = @At("RETURN"))
    private boolean toroidal$boundsThroughSeam(boolean original, double x, double z, double margin) {
        WorldFold transformer = this.toroidal$transformer;
        if (!transformer.isWrapped()) {
            return original;
        }

        return toroidal$insideAxis(transformer.blockDomain(Direction.Axis.X), getMinX(), getMaxX(), x, margin)
                && toroidal$insideAxis(transformer.blockDomain(Direction.Axis.Z), getMinZ(), getMaxZ(), z, margin);
    }

    @ModifyReturnValue(method = "getDistanceToBorder(DD)D", at = @At("RETURN"))
    private double toroidal$distanceThroughSeam(double original, double x, double z) {
        WorldFold transformer = this.toroidal$transformer;
        if (!transformer.isWrapped()) {
            return original;
        }

        double xGap = toroidal$gapToAxisEdge(transformer.blockDomain(Direction.Axis.X), getMinX(), getMaxX(), x);
        double zGap = toroidal$gapToAxisEdge(transformer.blockDomain(Direction.Axis.Z), getMinZ(), getMaxZ(), z);
        return Math.min(xGap, zGap);
    }

    @ModifyReturnValue(method = "clampToBounds(DDD)Lnet/minecraft/core/BlockPos;", at = @At("RETURN"))
    private BlockPos toroidal$clampThroughSeam(BlockPos original, double x, double y, double z) {
        WorldFold transformer = this.toroidal$transformer;
        if (!transformer.isWrapped()) {
            return original;
        }

        WrapDomain xDomain = transformer.blockDomain(Direction.Axis.X);
        WrapDomain zDomain = transformer.blockDomain(Direction.Axis.Z);
        return BlockPos.containing(
                xDomain.wrap(toroidal$clampToAxis(xDomain, getMinX(), getMaxX(), x)),
                y,
                zDomain.wrap(toroidal$clampToAxis(zDomain, getMinZ(), getMaxZ(), z)));
    }

    @WrapMethod(method = "getCollisionShape")
    private VoxelShape toroidal$wallThroughSeam(Operation<VoxelShape> original) {
        WorldFold transformer = this.toroidal$transformer;
        if (!transformer.isWrapped()) {
            return original.call();
        }

        double minX = getMinX();
        double maxX = getMaxX();
        double minZ = getMinZ();
        double maxZ = getMaxZ();

        double[] bounds = this.toroidal$wallBounds;
        VoxelShape wall = this.toroidal$wall;
        if (wall == null || bounds == null
                || bounds[0] != minX || bounds[1] != maxX || bounds[2] != minZ || bounds[3] != maxZ) {
            wall = toroidal$buildWall(transformer, minX, maxX, minZ, maxZ);
            this.toroidal$wall = wall;
            this.toroidal$wallBounds = new double[] {minX, maxX, minZ, maxZ};
        }

        return wall;
    }

    @Unique
    private static VoxelShape toroidal$buildWall(WorldFold transformer,
            double minX, double maxX, double minZ, double maxZ) {
        VoxelShape wall = Shapes.INFINITY;
        for (double xShift : toroidal$copyShifts(transformer.blockDomain(Direction.Axis.X))) {
            for (double zShift : toroidal$copyShifts(transformer.blockDomain(Direction.Axis.Z))) {
                wall = Shapes.join(wall, Shapes.box(
                        Math.floor(minX + xShift), Double.NEGATIVE_INFINITY, Math.floor(minZ + zShift),
                        Math.ceil(maxX + xShift), Double.POSITIVE_INFINITY, Math.ceil(maxZ + zShift)),
                        BooleanOp.ONLY_FIRST);
            }
        }

        return wall;
    }

    @Unique
    private static double[] toroidal$copyShifts(WrapDomain domain) {
        return domain.domainLength == 0
                ? new double[] {0.0}
                : new double[] {-domain.domainLength, 0.0, domain.domainLength};
    }

    // Vanilla's own reading, x >= min - margin && x < max + margin, restated so the offset can be the folded one.
    @Unique
    private static boolean toroidal$insideAxis(WrapDomain domain, double min, double max, double coord, double margin) {
        if (domain.coversWorld(max - min)) {
            return true;
        }

        double half = (max - min) / 2.0;
        double offset = domain.foldDelta(coord - (min + max) / 2.0);
        return offset >= -half - margin && offset < half + margin;
    }

    @Unique
    private static double toroidal$gapToAxisEdge(WrapDomain domain, double min, double max, double coord) {
        double half = (max - min) / 2.0;
        double offset = domain.foldDelta(coord - (min + max) / 2.0);
        return half - Math.abs(offset);
    }

    @Unique
    private static double toroidal$clampToAxis(WrapDomain domain, double min, double max, double coord) {
        if (domain.coversWorld(max - min)) {
            return coord;
        }

        double nearestCentre = domain.unwrapAround(coord, (min + max) / 2.0);
        double half = (max - min) / 2.0;
        return Mth.clamp(coord, nearestCentre - half, nearestCentre + half - 1.0);
    }
}
