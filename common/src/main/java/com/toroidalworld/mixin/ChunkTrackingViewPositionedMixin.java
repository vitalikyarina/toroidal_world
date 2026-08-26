package com.toroidalworld.mixin;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.toroidalworld.accessors.TransformerHolder;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;

import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.world.level.ChunkPos;

@Mixin(ChunkTrackingView.Positioned.class)
public abstract class ChunkTrackingViewPositionedMixin implements TransformerHolder {
    @Unique
    private WorldFold toroidal$transformer = WorldFolds.NOOP;

    @Override
    public WorldFold toroidal$transformer() {
        return this.toroidal$transformer;
    }

    @Override
    public void toroidal$setTransformer(WorldFold transformer) {
        this.toroidal$transformer = transformer;
    }

    @Inject(method = "contains(IIZ)Z", at = @At("HEAD"), cancellable = true)
    private void toroidal$containsWrapped(int chunkX, int chunkZ, boolean includeNeighbors, CallbackInfoReturnable<Boolean> cir) {
        if (!this.toroidal$transformer.isWrapped()) {
            return;
        }

        if (this.toroidal$transformer.isOver(new ChunkPos(chunkX, chunkZ))) {
            cir.setReturnValue(false);
            return;
        }

        ChunkPos center = ((ChunkTrackingView.Positioned) (Object) this).center();
        int viewDistance = ((ChunkTrackingView.Positioned) (Object) this).viewDistance();

        ChunkPos unwrapped = this.toroidal$transformer.nearestCopy(center, new ChunkPos(chunkX, chunkZ));

        cir.setReturnValue(ChunkTrackingView.isWithinDistance(
                center.x(), center.z(), viewDistance, unwrapped.x(), unwrapped.z(), includeNeighbors));
    }

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

        for (int x = center.x() - radius; x <= center.x() + radius; x++) {
            for (int z = center.z() - radius; z <= center.z() + radius; z++) {
                if (ChunkTrackingView.isWithinDistance(center.x(), center.z(), viewDistance, x, z, true)) {
                    consumer.accept(this.toroidal$transformer.fold(new ChunkPos(x, z)));
                }
            }
        }
    }
}
