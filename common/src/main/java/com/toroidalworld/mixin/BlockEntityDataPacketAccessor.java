package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntityType;

// The fields are readable through getters, but vanilla hides the constructor behind create(BlockEntity) factories
// that demand a live block entity — this is the only route to an instance with a moved position.
@Mixin(ClientboundBlockEntityDataPacket.class)
public interface BlockEntityDataPacketAccessor {
    @Invoker("<init>")
    static ClientboundBlockEntityDataPacket toroidal$create(BlockPos pos, BlockEntityType<?> type, CompoundTag tag) {
        throw new AssertionError();
    }
}
