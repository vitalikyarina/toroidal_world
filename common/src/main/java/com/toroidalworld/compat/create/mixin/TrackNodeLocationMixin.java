package com.toroidalworld.compat.create.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.toroidalworld.compat.create.CreateTrackFold;
import com.toroidalworld.compat.create.CreateTrackFold.NodeKeyAxes;
import com.toroidalworld.compat.create.TrackNodeKeyFold;
import com.toroidalworld.core.WorldLoopTransformer;

import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

// One physical rail end, one node key. The key is an absolute coordinate in half-blocks and the graph is a HashMap on
// it, so a rail end reached from one side of the seam and a rail end reached from the other have to arrive at the same
// ints or the line never closes: the walk files a second node for ground that already has one, the lookup a signal
// does by physical coordinate misses whichever of the two it was not filed under, and which one that is depends on the
// order the track was laid.
//
// The seam plane is a node by construction. Create promotes a rail end to a graph node every 16 blocks, and the bounds
// of a looped world lie on a chunk edge, so a line crossing the seam always has a node exactly on it — the two names
// for that node being the last coordinate of one side and the first of the other.
//
// The fold cannot be taken in the constructor: Vec3i's coordinates are settled by the super call, and the dimension —
// the only route to the bounds a node folds by — is assigned afterwards. So it is taken at each of the three places
// where a finished node first knows which world it is in. in(Level) hands the level over directly, which is what a
// client with no integrated server has instead of a server to ask; in(ResourceKey) covers every node built with only a
// dimension key, the walk's own ends among them; and read covers what is already on disk, where a world saved before
// this fold existed still holds the out-of-bounds twin. Two of those run one inside the other and the fold is
// idempotent, so the second is a comparison and nothing else.
//
// A node reloaded from a legacy save may land on a key another node already holds. That is the same rail end under its
// two names, and TrackGraph.read then points both node indices at the surviving object, so the edges of both sides
// attach to it — the crossing closes on load rather than needing the track relaid.
@Mixin(value = TrackNodeLocation.class, remap = false)
public abstract class TrackNodeLocationMixin extends Vec3i implements TrackNodeKeyFold {
    @Shadow
    public ResourceKey<Level> dimension;

    protected TrackNodeLocationMixin(int x, int y, int z) {
        super(x, y, z);
    }

    @Override
    public void toroidal$foldNodeKey(@Nullable Level level) {
        WorldLoopTransformer transformer = CreateTrackFold.transformerOf(level, this.dimension);
        if (transformer == null) {
            return;
        }

        NodeKeyAxes axes = CreateTrackFold.nodeKeyAxes(transformer);
        if (!axes.x().isOver(getX()) && !axes.z().isOver(getZ())) {
            return;
        }

        setX(axes.x().wrap(getX()));
        setZ(axes.z().wrap(getZ()));
    }

    @Inject(method = "in(Lnet/minecraft/world/level/Level;)Lcom/simibubi/create/content/trains/graph/TrackNodeLocation;",
            at = @At("RETURN"))
    private void toroidal$foldKeyInLevel(Level level, CallbackInfoReturnable<TrackNodeLocation> cir) {
        toroidal$foldNodeKey(level);
    }

    @Inject(method = "in(Lnet/minecraft/resources/ResourceKey;)Lcom/simibubi/create/content/trains/graph/TrackNodeLocation;",
            at = @At("RETURN"))
    private void toroidal$foldKeyInDimension(ResourceKey<Level> dimension,
            CallbackInfoReturnable<TrackNodeLocation> cir) {
        toroidal$foldNodeKey(null);
    }

    // A location read with a null palette keeps a null dimension — Carriage's portal pivot is read that way — and has
    // no world to fold by. It is geometry rather than a key and never reaches the node map.
    @Inject(method = "read", at = @At("RETURN"))
    private static void toroidal$foldKeyOnRead(CallbackInfoReturnable<TrackNodeLocation> cir) {
        ((TrackNodeKeyFold) cir.getReturnValue()).toroidal$foldNodeKey(null);
    }
}
