package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.accessors.TransformerHolder;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.core.WrapDomain;

import net.minecraft.util.Mth;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// The border is a square named by a centre and a width, and vanilla measures everything about it by comparing a raw
// coordinate against centre ± width/2. On a world that closes, that square describes ground on one side of the seam and
// denies the same ground on the other: a player who walks out of the world at one edge and back in at the other is
// suddenly outside a border they never crossed, and takes damage for it.
//
// So the measurement is folded rather than the square moved: the distance from the centre to a point is taken the short
// way round, which is the same thing as measuring to whichever copy of the square lies nearest. Every reading vanilla
// takes funnels into four methods and they are all here — the six isWithinBounds overloads meet in one private
// primitive, the entity form of getDistanceToBorder meets the loose one, the whole clampToBounds family meets
// clampVec3ToBound, and the wall itself is one shape. isInsideCloseToBorder is built out of the first two and needs
// nothing of its own.
//
// The transformer is stamped on by ServerLevelMixin, which is the only place a border and its level are ever in the
// same room: the border is per-level saved data and holds no reference back. A client's border is never stamped, and
// must not be — the client is told the world is infinite, and its border arrives already laid into that frame by
// PacketTranslator.
//
// The centre and half-width are read back off getMinX/getMaxX rather than from getCenterX and getSize, because those
// two are not the same square: vanilla clamps the edges to absoluteMaxSize, and the edges are what every vanilla
// reading below actually uses.
@Mixin(WorldBorder.class)
public class WorldBorderMixin implements TransformerHolder {
    @Unique
    private WorldLoopTransformer toroidal$transformer = WorldLoopTransformer.NOOP;

    // The wall is rebuilt only when the border itself moves. A shrinking border moves every tick and vanilla rebuilds
    // its shape per call there too, but a stationary one is asked for the same nine-copy shape by every entity standing
    // at it, on every tick it stands there.
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
    public WorldLoopTransformer toroidal$transformer() {
        return this.toroidal$transformer;
    }

    @Override
    public void toroidal$setTransformer(WorldLoopTransformer transformer) {
        this.toroidal$transformer = transformer;
    }

    // The one primitive the six public isWithinBounds overloads — block, vector, chunk, box and both loose forms — all
    // end in, so folding it here is the whole of the question for every caller in the game.
    //
    // A box or a chunk asks about its two corners separately, and two corners of one object always fold together: they
    // could only be split where the fold itself flips, which is the antipode of the border's centre — half a world from
    // it, and therefore outside any border narrower than the world and inside any border at least as wide.
    @Inject(method = "isWithinBounds(DDD)Z", at = @At("HEAD"), cancellable = true)
    private void toroidal$boundsThroughSeam(double x, double z, double margin, CallbackInfoReturnable<Boolean> cir) {
        WorldLoopTransformer transformer = this.toroidal$transformer;
        if (!transformer.isWrapped()) {
            return;
        }

        cir.setReturnValue(
                toroidal$insideAxis(transformer.coords.x, getMinX(), getMaxX(), x, margin)
                        && toroidal$insideAxis(transformer.coords.z, getMinZ(), getMaxZ(), z, margin));
    }

    @Inject(method = "getDistanceToBorder(DD)D", at = @At("HEAD"), cancellable = true)
    private void toroidal$distanceThroughSeam(double x, double z, CallbackInfoReturnable<Double> cir) {
        WorldLoopTransformer transformer = this.toroidal$transformer;
        if (!transformer.isWrapped()) {
            return;
        }

        double xGap = toroidal$gapToAxisEdge(transformer.coords.x, getMinX(), getMaxX(), x);
        double zGap = toroidal$gapToAxisEdge(transformer.coords.z, getMinZ(), getMaxZ(), z);
        cir.setReturnValue(Math.min(xGap, zGap));
    }

    // Vanilla clamps to the one square it knows; the copy of that square nearest the point is the one the point was
    // measured against, so it is the one it is pushed back into. The result is folded home afterwards: a coordinate
    // naming a copy is a coordinate the rest of the server would have to wrap anyway, and both callers — a portal being
    // placed, a ray stopped at the wall — turn it straight into a position in the world.
    @Inject(method = "clampVec3ToBound(DDD)Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
    private void toroidal$clampThroughSeam(double x, double y, double z, CallbackInfoReturnable<Vec3> cir) {
        WorldLoopTransformer transformer = this.toroidal$transformer;
        if (!transformer.isWrapped()) {
            return;
        }

        cir.setReturnValue(new Vec3(
                transformer.coords.x.wrap(toroidal$clampToAxis(transformer.coords.x, getMinX(), getMaxX(), x)),
                y,
                transformer.coords.z.wrap(toroidal$clampToAxis(transformer.coords.z, getMinZ(), getMaxZ(), z))));
    }

    // The wall vanilla builds is everything outside its one square, and it is handed to the caller with no anchor to
    // say which entity is about to collide with it — so it cannot be folded toward anyone. It does not have to be: on a
    // world that closes the wall is periodic by nature, standing at every copy of the square at once. Subtracting the
    // copies instead of the one square is the same statement made in a form that needs no anchor, and it leaves both
    // call sites — an entity gathering its colliders, a clip stopped at the border — reading the vanilla method.
    //
    // Three copies per wrapping axis reach any entity in the world: the copies sit a world width apart and every entity
    // is inside the world, so nothing can be nearer than the one before or after the canonical square. An axis that
    // does not close has the single square vanilla drew.
    //
    // A border at least as wide as the world falls out of this rather than being special-cased: its copies overlap and
    // cover everything, so what is left of the wall is nothing at all.
    @Inject(method = "getCollisionShape()Lnet/minecraft/world/phys/shapes/VoxelShape;", at = @At("HEAD"),
            cancellable = true)
    private void toroidal$wallThroughSeam(CallbackInfoReturnable<VoxelShape> cir) {
        WorldLoopTransformer transformer = this.toroidal$transformer;
        if (!transformer.isWrapped()) {
            return;
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

        cir.setReturnValue(wall);
    }

    @Unique
    private static VoxelShape toroidal$buildWall(WorldLoopTransformer transformer,
            double minX, double maxX, double minZ, double maxZ) {
        VoxelShape wall = Shapes.INFINITY;
        for (double xShift : toroidal$copyShifts(transformer.coords.x)) {
            for (double zShift : toroidal$copyShifts(transformer.coords.z)) {
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

    // Vanilla's own reading — x >= min - margin && x < max + margin — restated around the centre, so that the offset
    // from the centre can be the folded one. The half-open upper edge and the asymmetry of the margin are kept exactly
    // as vanilla wrote them.
    //
    // A square at least as wide as the world covers every copy of every point and is answered before the fold, which
    // otherwise leaves a single point unaccounted for: the antipode of the centre folds to exactly half a world, and a
    // border exactly the width of the world would read it as sitting on its own excluded upper edge.
    @Unique
    private static boolean toroidal$insideAxis(WrapDomain domain, double min, double max, double coord, double margin) {
        if (domain.coversWorld(max - min)) {
            return true;
        }

        double half = (max - min) / 2.0;
        double offset = domain.foldDelta(coord - (min + max) / 2.0);
        return offset >= -half - margin && offset < half + margin;
    }

    // How far the point is from the nearer of the two edges on this axis, negative once it is past them — vanilla's
    // min(coord - min, max - coord) with the offset folded.
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
        return Mth.clamp(coord, nearestCentre - half, nearestCentre + half - 1.0E-5F);
    }
}
