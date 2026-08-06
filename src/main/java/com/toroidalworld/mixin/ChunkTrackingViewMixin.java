package com.toroidalworld.mixin;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.toroidalworld.accessors.TransformerHolder;
import com.toroidalworld.core.WorldLoopTransformer;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.world.level.ChunkPos;

// Crossing the seam flips the player's chunk from one edge of the world to the other. To vanilla the old and the new
// view then look like two distant squares that do not overlap, so it drops every tracked chunk and reloads the whole
// view. Unwrapping the new centre around the old one puts both views back in one space, and the difference is again a
// thin strip of chunks — the same as any ordinary step.
@Mixin(ChunkTrackingView.class)
public interface ChunkTrackingViewMixin {
    @Inject(method = "difference", at = @At("HEAD"), cancellable = true)
    private static void toroidal$differenceWrapped(ChunkTrackingView from, ChunkTrackingView to,
            Consumer<ChunkPos> onEnter, Consumer<ChunkPos> onLeave, CallbackInfo ci) {
        if (!(from instanceof ChunkTrackingView.Positioned previous) || !(to instanceof ChunkTrackingView.Positioned next)) {
            return;
        }

        WorldLoopTransformer transformer = ((TransformerHolder) (Object) next).toroidal$transformer();
        if (!transformer.isWrapped()) {
            return;
        }

        ci.cancel();

        if (previous.equals(next)) {
            return;
        }

        ChunkPos previousCenter = previous.center();
        ChunkPos nextCenter = transformer.chunks.unwrap(previousCenter, next.center());

        int radius = Math.max(previous.viewDistance(), next.viewDistance()) + 1;
        int minX = Math.min(previousCenter.x(), nextCenter.x()) - radius;
        int minZ = Math.min(previousCenter.z(), nextCenter.z()) - radius;
        int maxX = Math.max(previousCenter.x(), nextCenter.x()) + radius;
        int maxZ = Math.max(previousCenter.z(), nextCenter.z()) + radius;

        // Membership is asked of the chunk itself, never of the coordinate it happens to be walked at: in a wrapped
        // world one chunk has several representations, and the same chunk can look far away as +15 while being right
        // next door as -17. Deciding per representation yields both an enter and a leave for one chunk, and whichever
        // came last wins — which is how a chunk the player is looking at gets dropped and leaves a hole.
        LongSet visited = new LongOpenHashSet();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                ChunkPos pos = new ChunkPos(transformer.chunks.x.wrap(x), transformer.chunks.z.wrap(z));
                if (!visited.add(pos.pack())) {
                    continue;
                }

                boolean saw = previous.contains(pos.x(), pos.z());
                boolean sees = next.contains(pos.x(), pos.z());
                if (saw == sees) {
                    continue;
                }

                if (sees) {
                    onEnter.accept(pos);
                } else {
                    onLeave.accept(pos);
                }
            }
        }
    }
}
