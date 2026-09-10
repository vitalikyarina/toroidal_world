package com.toroidalworld.mixin;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.toroidalworld.accessors.TransformerHolder;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

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

    @ModifyReturnValue(method = "contains(IIZ)Z", at = @At("RETURN"))
    private boolean toroidal$containsWrapped(boolean original, int chunkX, int chunkZ, boolean includeNeighbors) {
        if (!this.toroidal$transformer.isWrapped()) {
            return original;
        }

        if (this.toroidal$transformer.isOver(new ChunkPos(chunkX, chunkZ))) {
            return false;
        }

        ChunkPos center = ((ChunkTrackingView.Positioned) (Object) this).center();
        int viewDistance = ((ChunkTrackingView.Positioned) (Object) this).viewDistance();

        ChunkPos unwrapped = this.toroidal$transformer.nearestCopy(center, new ChunkPos(chunkX, chunkZ));

        return ChunkTrackingView.isWithinDistance(
                center.x, center.z, viewDistance, unwrapped.x, unwrapped.z, includeNeighbors);
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

        for (int x = center.x - radius; x <= center.x + radius; x++) {
            for (int z = center.z - radius; z <= center.z + radius; z++) {
                if (ChunkTrackingView.isWithinDistance(center.x, center.z, viewDistance, x, z, true)) {
                    consumer.accept(this.toroidal$transformer.fold(new ChunkPos(x, z)));
                }
            }
        }
    }
}
