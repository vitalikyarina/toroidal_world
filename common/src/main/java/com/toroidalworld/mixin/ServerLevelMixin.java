package com.toroidalworld.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.toroidalworld.accessors.LevelBindable;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.net.ListenerCopies;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.player.SeamSnap;
import com.toroidalworld.storage.SeamRespawnData;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.phys.Vec3;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {
    @Shadow
    @Final
    private PersistentEntitySectionManager<Entity> entityManager;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void toroidal$bindLevelToTickContainers(CallbackInfo ci) {
        ServerLevel level = (ServerLevel) (Object) this;
        ((LevelBindable) level.getBlockTicks()).toroidal$bindLevel(level);
        ((LevelBindable) level.getFluidTicks()).toroidal$bindLevel(level);
        ((LevelBindable) level.getRaids()).toroidal$bindLevel(level);
        ((LevelBindable) this.entityManager).toroidal$bindLevel(level);
    }

    @Inject(method = "tickNonPassenger", at = @At("TAIL"))
    private void toroidal$wrapEntityIntoBounds(Entity entity, CallbackInfo ci) {
        if (entity instanceof Player) {
            return;
        }

        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf((ServerLevel) (Object) this);
        if (transformer == null) {
            return;
        }

        if (transformer.isOver(entity.position())) {
            Vec3 wrapped = transformer.fold(entity.position());
            SeamSnap.withPassengers(entity, wrapped.subtract(entity.position()));
        }
    }

    @ModifyVariable(method = "setDefaultSpawnPos(Lnet/minecraft/core/BlockPos;F)V", at = @At("HEAD"), argsOnly = true)
    private BlockPos toroidal$storeWorldSpawnInsideBounds(BlockPos spawnPos) {
        return SeamRespawnData.insideBounds((ServerLevel) (Object) this, spawnPos);
    }

    @WrapMethod(method = "sendParticles(Lnet/minecraft/server/level/ServerPlayer;ZDDDLnet/minecraft/network/protocol/Packet;)Z")
    private boolean toroidal$particlesThroughSeam(ServerPlayer player, boolean overrideLimiter, double x, double y, double z,
            Packet<?> packet, Operation<Boolean> original) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf((ServerLevel) (Object) this);
        if (transformer == null) {
            return original.call(player, overrideLimiter, x, y, z, packet);
        }

        Vec3 nearest = transformer.nearestCopy(player.position(), new Vec3(x, y, z));
        return original.call(player, overrideLimiter, nearest.x, nearest.y, nearest.z, packet);
    }

    @WrapMethod(method = "destroyBlockProgress")
    private void toroidal$blockCracksThroughSeam(int id, BlockPos blockPos, int progress, Operation<Void> original) {
        ServerLevel level = (ServerLevel) (Object) this;
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        if (transformer == null) {
            original.call(id, blockPos, progress);
            return;
        }

        List<BlockPos> copies = ListenerCopies.nearestTo(transformer, level.getServer().getPlayerList().getPlayers(),
                player -> player.level() == level && player.getId() != id, blockPos);
        for (BlockPos copy : copies) {
            original.call(id, copy, progress);
        }
    }

    @WrapMethod(method = "tickPrecipitation")
    private void toroidal$bindPrecipitationTransformer(BlockPos pos, Operation<Void> original) {
        ServerLevel level = (ServerLevel) (Object) this;
        GenerationTransformerContext.runWithTransformer(
                WorldLoopAttachments.transformerOf(level), () -> original.call(pos));
    }
}
