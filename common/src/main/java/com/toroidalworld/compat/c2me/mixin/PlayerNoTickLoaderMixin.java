package com.toroidalworld.compat.c2me.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.accessors.LevelHolder;
import com.toroidalworld.compat.c2me.C2meSeamFold;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.ishland.c2me.notickvd.common.PlayerNoTickLoader;
import com.ishland.c2me.rewrites.chunksystem.common.TheChunkSystem;
import com.ishland.flowsched.scheduler.ItemHolder;
import com.ishland.flowsched.scheduler.ItemStatus;
import com.ishland.flowsched.scheduler.ItemTicket;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.ChunkPos;

@Mixin(PlayerNoTickLoader.class)
public class PlayerNoTickLoaderMixin {
    @Shadow
    @Final
    private ChunkMap tacs;

    @Unique
    private boolean toroidal$resolved;

    @Unique
    private @Nullable WorldFold toroidal$transformer;

    @Unique
    private @Nullable WorldFold toroidal$transformer() {
        if (!this.toroidal$resolved) {
            this.toroidal$transformer =
                    WorldLoopAttachments.wrappedTransformerOf(((LevelHolder) (Object) this.tacs).toroidal$level());
            this.toroidal$resolved = true;
        }

        return this.toroidal$transformer;
    }

    @WrapOperation(
            method = "loadChunk0",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/ishland/c2me/rewrites/chunksystem/common/TheChunkSystem;addTicket(Ljava/lang/Object;Lcom/ishland/flowsched/scheduler/ItemTicket$TicketType;Ljava/lang/Object;Lcom/ishland/flowsched/scheduler/ItemStatus;Ljava/lang/Runnable;)Lcom/ishland/flowsched/scheduler/ItemHolder;"))
    private ItemHolder<?, ?, ?, ?> toroidal$ticketCanonicalChunk(
            TheChunkSystem chunkSystem,
            Object key,
            ItemTicket.TicketType type,
            Object source,
            ItemStatus<?, ?, ?> status,
            Runnable callback,
            Operation<ItemHolder<?, ?, ?, ?>> original) {
        return original.call(chunkSystem, this.toroidal$canonical(key), type, source, status, callback);
    }

    @WrapOperation(
            method = "removeTicket0",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/ishland/c2me/rewrites/chunksystem/common/TheChunkSystem;removeTicket(Ljava/lang/Object;Lcom/ishland/flowsched/scheduler/ItemTicket$TicketType;Ljava/lang/Object;Lcom/ishland/flowsched/scheduler/ItemStatus;)V"))
    private void toroidal$untickedCanonicalChunk(
            TheChunkSystem chunkSystem,
            Object key,
            ItemTicket.TicketType type,
            Object source,
            ItemStatus<?, ?, ?> status,
            Operation<Void> original) {
        original.call(chunkSystem, this.toroidal$canonical(key), type, source, status);
    }

    @Unique
    private Object toroidal$canonical(Object key) {
        WorldFold transformer = this.toroidal$transformer();
        if (transformer == null || !(key instanceof ChunkPos pos)) {
            return key;
        }

        return C2meSeamFold.canonical(transformer, pos.x, pos.z);
    }

    @ModifyVariable(method = "setViewDistance", at = @At("HEAD"), argsOnly = true)
    private int toroidal$clampNoTickViewDistance(int viewDistance) {
        WorldFold transformer = this.toroidal$transformer();
        return transformer == null ? viewDistance : transformer.limitViewDistance(viewDistance);
    }
}
