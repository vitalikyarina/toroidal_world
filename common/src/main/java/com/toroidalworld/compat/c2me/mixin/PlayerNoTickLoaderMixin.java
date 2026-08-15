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
import com.toroidalworld.core.WorldLoopTransformer;
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

// The second chunk loader. No-tick view distance walks a spiral around each player and tickets every position it
// yields directly into the chunk system — past the vanilla ticket graph the mod folds in ChunkTrackerMixin, which is
// why holders kept appearing past the bounds however much of C2ME's own chunk system was already folded. The spiral
// runs in raw coordinates by design; what it names past the bounds is the ground across the seam.
//
// The chunk the ticket lands on is folded; the ticket's SOURCE is left raw on purpose. Two players on opposite sides
// of the seam can want the same physical chunk under two raw names, and a ticket identity of (type, source, status)
// keeps those two apart — folded, the second player's arrival would be a no-op and the first player's departure would
// drop the chunk out from under them.
@Mixin(PlayerNoTickLoader.class)
public class PlayerNoTickLoaderMixin {
    @Shadow
    @Final
    private ChunkMap tacs;

    @Unique
    private boolean toroidal$resolved;

    @Unique
    private @Nullable WorldLoopTransformer toroidal$transformer;

    @Unique
    private @Nullable WorldLoopTransformer toroidal$transformer() {
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
        WorldLoopTransformer transformer = this.toroidal$transformer();
        if (transformer == null || !(key instanceof ChunkPos pos)) {
            return key;
        }

        return C2meSeamFold.canonical(transformer, pos.x, pos.z);
    }

    // The other writer of the radius the client's mapping depends on. A chunk is shown at its representation nearest
    // the player, which is unambiguous only while everything the player holds is closer than half the world — the
    // invariant ChunkMapMixin.getPlayerViewDistance enforces for the ticking distance. This loader decides a second
    // radius of its own, and it reaches much further by design (its own ceiling is 65536 chunks), so the same ceiling
    // is applied here. The player's render distance setting is untouched: what is bounded is how far this mod lets
    // the world be loaded around them, which is this mod's decision and not the client's.
    @ModifyVariable(method = "setViewDistance", at = @At("HEAD"), argsOnly = true)
    private int toroidal$clampNoTickViewDistance(int viewDistance) {
        WorldLoopTransformer transformer = this.toroidal$transformer();
        return transformer == null ? viewDistance : transformer.limitViewDistance(viewDistance);
    }
}
