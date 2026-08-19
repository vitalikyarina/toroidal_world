package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

import com.toroidalworld.accessors.LevelBindable;
import com.toroidalworld.accessors.LevelBindRegistry;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.TicketStorage;

@Mixin(TicketStorage.class)
public class TicketStorageMixin implements LevelBindable, LevelBindRegistry {
    @Unique
    private @Nullable ServerLevel toroidal$level;

    // The loading graph registers itself here at construction (LoadingChunkTrackerMixin) — its class is
    // package-private, so the bind cannot reach it through a shadowed field the way the public trackers are reached.
    @Unique
    private final List<LevelBindable> toroidal$registeredBindables = new ArrayList<>();

    @Override
    public void toroidal$registerBindable(LevelBindable bindable) {
        this.toroidal$registeredBindables.add(bindable);
    }

    @Override
    public void toroidal$bindLevel(ServerLevel level) {
        this.toroidal$level = level;
        for (LevelBindable bindable : this.toroidal$registeredBindables) {
            bindable.toroidal$bindLevel(level);
        }
    }

    @ModifyVariable(method = "addTicket(JLnet/minecraft/server/level/Ticket;)Z", at = @At("HEAD"), argsOnly = true)
    private long toroidal$foldAddedTicketKey(long key) {
        return this.toroidal$foldKey(key);
    }

    @ModifyVariable(method = "removeTicket(JLnet/minecraft/server/level/Ticket;)Z", at = @At("HEAD"), argsOnly = true)
    private long toroidal$foldRemovedTicketKey(long key) {
        return this.toroidal$foldKey(key);
    }

    @Unique
    private long toroidal$foldKey(long key) {
        if (this.toroidal$level == null) {
            return key;
        }

        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(this.toroidal$level);
        if (transformer == null) {
            return key;
        }

        int chunkX = ChunkPos.getX(key);
        int chunkZ = ChunkPos.getZ(key);
        int wrappedX = transformer.chunks.x.wrap(chunkX);
        int wrappedZ = transformer.chunks.z.wrap(chunkZ);
        return wrappedX == chunkX && wrappedZ == chunkZ ? key : ChunkPos.pack(wrappedX, wrappedZ);
    }
}
