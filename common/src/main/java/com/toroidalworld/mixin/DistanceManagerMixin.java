package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.accessors.LevelBindable;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.LoadingChunkTracker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.SimulationChunkTracker;

@Mixin(DistanceManager.class)
public class DistanceManagerMixin implements LevelBindable {
    @Shadow
    @Final
    private LoadingChunkTracker loadingChunkTracker;

    @Shadow
    @Final
    private SimulationChunkTracker simulationChunkTracker;

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
        ((LevelBindable) this.loadingChunkTracker).toroidal$bindLevel(level);
        ((LevelBindable) this.simulationChunkTracker).toroidal$bindLevel(level);
        ((LevelBindable) this.naturalSpawnChunkCounter).toroidal$bindLevel(level);
        ((LevelBindable) this.playerTicketManager).toroidal$bindLevel(level);
    }

    @ModifyVariable(method = "inEntityTickingRange", at = @At("HEAD"), argsOnly = true)
    private long toroidal$entityTickingOnPhysicalChunk(long chunkKey) {
        if (this.toroidal$level == null) {
            return chunkKey;
        }

        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(this.toroidal$level);
        return transformer == null ? chunkKey : transformer.chunks.wrapChunkKey(chunkKey);
    }
}
