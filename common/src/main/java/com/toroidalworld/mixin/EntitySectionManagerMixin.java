package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.toroidalworld.accessors.LevelBindable;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.player.SeamSnap;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.phys.Vec3;

@Mixin(PersistentEntitySectionManager.class)
public class EntitySectionManagerMixin implements LevelBindable {
    @Unique
    private @Nullable ServerLevel toroidal$level;

    @Override
    public void toroidal$bindLevel(ServerLevel level) {
        this.toroidal$level = level;
    }

    @Inject(
            method = {
                    "addEntity(Lnet/minecraft/world/level/entity/EntityAccess;Z)Z",
                    "addEntityWithoutEvent(Lnet/minecraft/world/level/entity/EntityAccess;Z)Z" },
            at = @At("HEAD"),
            require = 1)
    private void toroidal$wrapJoiningEntity(EntityAccess entity, boolean loaded, CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof Entity actualEntity)) {
            return;
        }

        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(actualEntity.level());
        if (transformer == null) {
            return;
        }

        Vec3 position = actualEntity.position();
        if (!transformer.isOver(position)) {
            return;
        }

        Vec3 wrapped = transformer.fold(position);
        SeamSnap.withPassengers(actualEntity, wrapped.subtract(position));
    }

    @ModifyArg(
            method = {"canPositionTick", "areEntitiesLoaded"},
            at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;get(J)Ljava/lang/Object;"),
            index = 0,
            expect = 3)
    private long toroidal$askThePhysicalChunk(long chunkKey) {
        if (this.toroidal$level == null) {
            return chunkKey;
        }

        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(this.toroidal$level);
        return transformer == null ? chunkKey : transformer.foldChunkKey(chunkKey);
    }
}
