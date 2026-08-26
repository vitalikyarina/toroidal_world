package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.accessors.LevelBindable;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TickingTracker;

@Mixin(DistanceManager.class)
public class DistanceManagerMixin implements LevelBindable {
    @Shadow
    @Final
    private DistanceManager.ChunkTicketTracker ticketTracker;

    @Shadow
    @Final
    private TickingTracker tickingTicketsTracker;

    @Shadow
    @Final
    private DistanceManager.FixedPlayerDistanceChunkTracker naturalSpawnChunkCounter;

    @Shadow
    @Final
    private DistanceManager.PlayerTicketTracker playerTicketManager;

    @Unique
    private @Nullable ServerLevel toroidal$level;

    @Override
    public void toroidal$bindLevel(ServerLevel level) {
        this.toroidal$level = level;
        ((LevelBindable) this.ticketTracker).toroidal$bindLevel(level);
        ((LevelBindable) this.tickingTicketsTracker).toroidal$bindLevel(level);
        ((LevelBindable) this.naturalSpawnChunkCounter).toroidal$bindLevel(level);
        ((LevelBindable) this.playerTicketManager).toroidal$bindLevel(level);
    }

    @ModifyVariable(method = "addTicket(JLnet/minecraft/server/level/Ticket;)V", at = @At("HEAD"), argsOnly = true)
    private long toroidal$foldAddedTicketKey(long key) {
        return this.toroidal$foldKey(key);
    }

    @ModifyVariable(method = "removeTicket(JLnet/minecraft/server/level/Ticket;)V", at = @At("HEAD"), argsOnly = true)
    private long toroidal$foldRemovedTicketKey(long key) {
        return this.toroidal$foldKey(key);
    }

    @ModifyVariable(method = "inEntityTickingRange", at = @At("HEAD"), argsOnly = true)
    private long toroidal$entityTickingOnPhysicalChunk(long chunkKey) {
        return this.toroidal$foldKey(chunkKey);
    }

    @Unique
    private long toroidal$foldKey(long key) {
        WorldFold transformer = this.toroidal$wrappedTransformer();
        return transformer == null ? key : transformer.foldChunkKey(key);
    }

    @Unique
    private @Nullable WorldFold toroidal$wrappedTransformer() {
        return this.toroidal$level == null ? null : WorldLoopAttachments.wrappedTransformerOf(this.toroidal$level);
    }
}
