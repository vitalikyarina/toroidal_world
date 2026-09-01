package com.toroidalworld.mixin;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.toroidalworld.accessors.TransformerHolder;
import com.toroidalworld.core.ChunkViewSweep;
import com.toroidalworld.core.WorldFold;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.world.level.ChunkPos;

@Mixin(ChunkTrackingView.class)
public interface ChunkTrackingViewMixin {
    @Inject(method = "difference", at = @At("HEAD"), cancellable = true)
    private static void toroidal$differenceWrapped(ChunkTrackingView from, ChunkTrackingView to,
            Consumer<ChunkPos> onEnter, Consumer<ChunkPos> onLeave, CallbackInfo ci) {
        if (!(from instanceof ChunkTrackingView.Positioned previous) || !(to instanceof ChunkTrackingView.Positioned next)) {
            return;
        }

        WorldFold transformer = ((TransformerHolder) (Object) next).toroidal$transformer();
        if (!transformer.isWrapped()) {
            return;
        }

        ci.cancel();

        if (previous.equals(next)) {
            return;
        }

        ChunkViewSweep sweep = ChunkViewSweep.between(
                transformer, previous.center(), previous.viewDistance(), next.center(), next.viewDistance());

        if (!(sweep instanceof ChunkViewSweep.OverBoth box)) {
            previous.forEach(onLeave);
            next.forEach(onEnter);
            return;
        }

        LongSet visited = new LongOpenHashSet();

        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                ChunkPos pos = transformer.fold(new ChunkPos(x, z));
                if (!visited.add(pos.toLong())) {
                    continue;
                }

                boolean saw = previous.contains(pos.x, pos.z);
                boolean sees = next.contains(pos.x, pos.z);
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
