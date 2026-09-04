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
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.TicketStorage;

@Mixin(TicketStorage.class)
public class TicketStorageMixin implements LevelBindable, LevelBindRegistry {
    @Unique
    private @Nullable ServerLevel toroidal$level;

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
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(this.toroidal$level);
        return transformer == null ? key : transformer.foldChunkKey(key);
    }
}
