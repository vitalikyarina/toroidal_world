package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

// The per-tick canvas refresh samples a window of columns around the map's holder, and the window is placed from the
// raw delta between the holder and the map center: a holder across the seam from the mapped area computes an
// off-canvas window and the refresh silently stops — including the moment a player carrying the map crosses the seam
// and their coordinate jumps a world width. The holder is read at their copy nearest the center, so the window lands
// where the holder visually stands. The sampled columns themselves need no fold: every column's chunk access already
// wraps on its way through Level.getChunk, and the chunk-local height and block reads index by the low four bits.
@Mixin(MapItem.class)
public class MapItemMixin {
    @WrapOperation(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getX()D"))
    private double toroidal$holderNearestCenterX(Entity holder, Operation<Double> original,
            @Local(argsOnly = true) MapItemSavedData data) {
        double x = original.call(holder);
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(holder.level());
        return transformer == null ? x : transformer.coords.x.unwrapAround(data.centerX, x);
    }

    @WrapOperation(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getZ()D"))
    private double toroidal$holderNearestCenterZ(Entity holder, Operation<Double> original,
            @Local(argsOnly = true) MapItemSavedData data) {
        double z = original.call(holder);
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(holder.level());
        return transformer == null ? z : transformer.coords.z.unwrapAround(data.centerZ, z);
    }

    // The stale-banner sweep compares stored banner positions against the sampled column's raw coordinates, which run
    // past the bounds for the part of the canvas across the seam — folded into the world they name the column's real
    // ground, the coordinates a banner position can actually match.
    @WrapOperation(
            method = "update",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/saveddata/maps/MapItemSavedData;checkBanners(Lnet/minecraft/world/level/BlockGetter;II)V"))
    private void toroidal$checkBannersInWorld(MapItemSavedData data, BlockGetter blockGetter, int x, int z,
            Operation<Void> original, @Local(argsOnly = true) Level level) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        if (transformer == null) {
            original.call(data, blockGetter, x, z);
            return;
        }

        original.call(data, blockGetter, transformer.coords.x.wrap(x), transformer.coords.z.wrap(z));
    }
}
