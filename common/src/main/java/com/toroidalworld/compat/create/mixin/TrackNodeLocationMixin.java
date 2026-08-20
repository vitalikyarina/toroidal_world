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

    @Inject(method = "read", at = @At("RETURN"))
    private static void toroidal$foldKeyOnRead(CallbackInfoReturnable<TrackNodeLocation> cir) {
        ((TrackNodeKeyFold) cir.getReturnValue()).toroidal$foldNodeKey(null);
    }
}
