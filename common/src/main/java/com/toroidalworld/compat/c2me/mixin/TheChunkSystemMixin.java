package com.toroidalworld.compat.c2me.mixin;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.toroidalworld.accessors.LevelHolder;
import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.accessors.TransformerSourceBindable;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.ishland.c2me.base.common.scheduler.SchedulingManager;
import com.ishland.c2me.rewrites.chunksystem.common.TheChunkSystem;
import com.ishland.flowsched.scheduler.ItemHolder;
import com.ishland.flowsched.scheduler.ItemStatus;
import com.mojang.logging.LogUtils;

import net.minecraft.server.level.ChunkMap;

// One chunk system per level, so the transformer is resolved once and read off a field afterwards — the dependency
// sets this feeds are recomputed on every status transition of every chunk, which is no place for an attachment
// lookup. The level itself comes through the mod's own duck on ChunkMap.
@Mixin(TheChunkSystem.class)
public class TheChunkSystemMixin implements TransformerSource {
    @Shadow
    @Final
    private ChunkMap tacs;

    @Shadow
    @Final
    private SchedulingManager schedulingManager;

    // The worldgen write lock is taken with nothing but this manager in hand, so it is given the one object per level
    // that answers the question — bound here rather than resolved there, because the answer is still lazy and this is
    // where the two meet.
    @Inject(method = "<init>", at = @At("TAIL"))
    private void toroidal$bindLockFoldSource(ChunkMap tacs, CallbackInfo ci) {
        ((TransformerSourceBindable) this.schedulingManager).toroidal$bindTransformerSource(this);
    }

    @Unique
    private boolean toroidal$resolved;

    @Unique
    private @Nullable WorldLoopTransformer toroidal$transformer;

    @Override
    public @Nullable WorldLoopTransformer toroidal$wrappedTransformer() {
        if (!this.toroidal$resolved) {
            this.toroidal$transformer =
                    WorldLoopAttachments.wrappedTransformerOf(((LevelHolder) (Object) this.tacs).toroidal$level());
            this.toroidal$resolved = true;
        }

        return this.toroidal$transformer;
    }

    @Unique
    private static final Logger toroidal$LOGGER = LogUtils.getLogger();

    // Diagnostic. C2ME logs this failure with three placeholders and three arguments plus the throwable, so slf4j
    // consumes the throwable as an argument and the stack is never printed — the run says "NullPointerException" and
    // nothing about where. This prints it properly.
    @Inject(method = "handleTransactionException", at = @At("HEAD"))
    private void toroidal$logTransactionFailure(
            ItemHolder<?, ?, ?, ?> holder,
            ItemStatus<?, ?, ?> nextStatus,
            boolean isUpgrade,
            Throwable throwable,
            CallbackInfoReturnable<?> cir) {
        toroidal$LOGGER.warn("[c2me-compat] chunk_transaction_failed chunk={} status={} upgrade={}",
                holder.getKey(), nextStatus, isUpgrade, throwable);
    }
}
