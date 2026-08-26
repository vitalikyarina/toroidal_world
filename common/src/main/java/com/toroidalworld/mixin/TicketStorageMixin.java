package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.accessors.LevelBindable;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.TicketStorage;

@Mixin(TicketStorage.class)
public class TicketStorageMixin implements LevelBindable {
    @Unique
    private @Nullable ServerLevel toroidal$level;

    @Override
    public void toroidal$bindLevel(ServerLevel level) {
        this.toroidal$level = level;
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

        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(this.toroidal$level);
        if (transformer == null) {
            return key;
        }

        return transformer.foldChunkKey(key);
    }
}
