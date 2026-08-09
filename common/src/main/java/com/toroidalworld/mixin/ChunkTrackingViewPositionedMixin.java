package com.toroidalworld.mixin;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.toroidalworld.accessors.TransformerHolder;
import com.toroidalworld.core.WorldLoopTransformer;

import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.world.level.ChunkPos;

// The view decides which chunks a player sees, by plain distance from their chunk. Across the seam that distance is a
// whole world wide, so the far side would never be tracked — every chunk position is therefore unwrapped around the
// centre first (its shortest representation through the seam) and only then measured.
@Mixin(ChunkTrackingView.Positioned.class)
public abstract class ChunkTrackingViewPositionedMixin implements TransformerHolder {
    @Unique
    private WorldLoopTransformer toroidal$transformer = WorldLoopTransformer.NOOP;

    @Override
    public WorldLoopTransformer toroidal$transformer() {
        return this.toroidal$transformer;
    }

    @Override
    public void toroidal$setTransformer(WorldLoopTransformer transformer) {
        this.toroidal$transformer = transformer;
    }

    @Inject(method = "contains(IIZ)Z", at = @At("HEAD"), cancellable = true)
    private void toroidal$containsWrapped(int chunkX, int chunkZ, boolean includeNeighbors, CallbackInfoReturnable<Boolean> cir) {
        if (!this.toroidal$transformer.isWrapped()) {
            return;
        }

        // Chunks past the wrap bounds are phantoms: vanilla keeps holders for them as generation neighbours, but their
        // generation is cancelled, so they are empty. Tracking one means sending an empty chunk to the client under the
        // very coordinate the real chunk on the other side is sent with — the phantom overwrites it and leaves terrain
        // that is invisible and has no collision, while the server happily walks its mobs over the real ground.
        if (this.toroidal$transformer.chunks.x.isOver(chunkX) || this.toroidal$transformer.chunks.z.isOver(chunkZ)) {
            cir.setReturnValue(false);
            return;
        }

        ChunkPos center = ((ChunkTrackingView.Positioned) (Object) this).center();
        int viewDistance = ((ChunkTrackingView.Positioned) (Object) this).viewDistance();

        int unwrappedX = this.toroidal$transformer.chunks.x.unwrap(center.x, chunkX);
        int unwrappedZ = this.toroidal$transformer.chunks.z.unwrap(center.z, chunkZ);

        cir.setReturnValue(ChunkTrackingView.isWithinDistance(
                center.x, center.z, viewDistance, unwrappedX, unwrappedZ, includeNeighbors));
    }

    // The square is walked in unwrapped space and each position wrapped back, because everything downstream (chunk
    // loading, the pending-send set) speaks the server's wrapped coordinates.
    @Inject(method = "forEach", at = @At("HEAD"), cancellable = true)
    private void toroidal$forEachWrapped(Consumer<ChunkPos> consumer, CallbackInfo ci) {
        if (!this.toroidal$transformer.isWrapped()) {
            return;
        }

        ci.cancel();

        ChunkTrackingView.Positioned view = (ChunkTrackingView.Positioned) (Object) this;
        ChunkPos center = view.center();
        int viewDistance = view.viewDistance();
        int radius = viewDistance + 1;

        for (int x = center.x - radius; x <= center.x + radius; x++) {
            for (int z = center.z - radius; z <= center.z + radius; z++) {
                if (ChunkTrackingView.isWithinDistance(center.x, center.z, viewDistance, x, z, true)) {
                    consumer.accept(new ChunkPos(
                            this.toroidal$transformer.chunks.x.wrap(x),
                            this.toroidal$transformer.chunks.z.wrap(z)));
                }
            }
        }
    }
}
