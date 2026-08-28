package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import com.simibubi.create.content.equipment.blueprint.BlueprintEntity;
import com.toroidalworld.compat.create.CreateSeamFold;
import com.toroidalworld.compat.create.CreateTrackFold;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = BlueprintEntity.class, remap = false)
public class BlueprintEntityMixin {
    @Unique
    private static final String TILE_X_KEY = "TileX";
    @Unique
    private static final String TILE_Y_KEY = "TileY";
    @Unique
    private static final String TILE_Z_KEY = "TileZ";

    @ModifyExpressionValue(method = "readSpawnData",
            at = @At(value = "INVOKE", ordinal = 0,
                    target = "Lnet/minecraft/network/RegistryFriendlyByteBuf;"
                            + "readNbt()Lnet/minecraft/nbt/CompoundTag;"))
    private CompoundTag toroidal$attachmentInTheBlueprintFrame(CompoundTag spawnData) {
        if (spawnData == null || !spawnData.contains(TILE_X_KEY)) {
            return spawnData;
        }

        Entity blueprint = (Entity) (Object) this;
        BlockPos canonical = new BlockPos(spawnData.getInt(TILE_X_KEY), spawnData.getInt(TILE_Y_KEY),
                spawnData.getInt(TILE_Z_KEY));
        BlockPos folded = CreateTrackFold.nearestCopy(blueprint.level(), blueprint.blockPosition(), canonical);
        if (folded.equals(canonical)) {
            return spawnData;
        }

        spawnData.putInt(TILE_X_KEY, folded.getX());
        spawnData.putInt(TILE_Y_KEY, folded.getY());
        spawnData.putInt(TILE_Z_KEY, folded.getZ());
        return spawnData;
    }

    @ModifyVariable(method = "skipAttackInteraction", at = @At("STORE"), ordinal = 0)
    private Vec3 toroidal$foldAttackRay(Vec3 eyePos, Entity source) {
        Entity blueprint = (Entity) (Object) this;
        return CreateSeamFold.foldPoint(blueprint.level(), blueprint.position(), eyePos);
    }
}
