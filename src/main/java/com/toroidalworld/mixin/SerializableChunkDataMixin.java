package com.toroidalworld.mixin;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.google.common.collect.Maps;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.logging.LogUtils;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.minecraft.world.level.levelgen.structure.Structure;

// A chunk read back from disk has every structure reference sitting further than the scan radius thrown away as
// corrupt. The distance is measured between the two canonical positions, and a reference folded across the seam is
// canonically a whole world away — so the load path quietly deletes exactly the references this dimension went to the
// trouble of establishing, and writes the chunk back without them the next time it saves.
//
// Restated rather than filtered afterwards: by the time the original returns, the discarded entries are gone. The
// threshold stays vanilla's, only the metric is folded — a reference genuinely too far is still corrupt and still goes.
@Mixin(SerializableChunkData.class)
public class SerializableChunkDataMixin {
    @Unique
    private static final Logger toroidal$LOGGER = LogUtils.getLogger();

    // Vanilla's own literal, restated because the code it lives in is not reachable from here. It is the reference scan
    // radius: no chunk can hold a reference to a start further out than the scan that files them reaches.
    @Unique
    private static final int toroidal$MAX_REFERENCE_DISTANCE = 8;

    @Unique
    private static final String toroidal$REFERENCES_KEY = "References";

    // Vanilla-body re-implementation — verified against 26.2; re-diff on a platform bump.
    @WrapOperation(
            method = "read",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/storage/SerializableChunkData;unpackStructureReferences(Lnet/minecraft/core/RegistryAccess;Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/nbt/CompoundTag;)Ljava/util/Map;"))
    private Map<Structure, LongSet> toroidal$keepReferencesAcrossTheSeam(
            RegistryAccess registryAccess,
            ChunkPos pos,
            CompoundTag tag,
            Operation<Map<Structure, LongSet>> original,
            @Local(argsOnly = true) ServerLevel level) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        if (transformer == null) {
            return original.call(registryAccess, pos, tag);
        }

        Map<Structure, LongSet> references = Maps.newHashMap();
        Registry<Structure> structures = registryAccess.lookupOrThrow(Registries.STRUCTURE);
        tag.getCompoundOrEmpty(toroidal$REFERENCES_KEY).forEach((key, entry) -> {
            Identifier structureId = Identifier.tryParse(key);
            Structure structure = structures.getValue(structureId);
            if (structure == null) {
                toroidal$LOGGER.warn("Found reference to unknown structure '{}' in chunk {}, discarding", structureId, pos);
                return;
            }

            Optional<long[]> stored = entry.asLongArray();
            if (stored.isEmpty()) {
                return;
            }

            LongSet kept = new LongOpenHashSet();
            for (long referenceKey : stored.get()) {
                ChunkPos referencePos = ChunkPos.unpack(referenceKey);
                if (transformer.chunks.chessboardDistance(pos, referencePos) > toroidal$MAX_REFERENCE_DISTANCE) {
                    toroidal$LOGGER.warn(
                            "Found invalid structure reference [ {} @ {} ] for chunk {}.", structureId, referencePos, pos);
                    continue;
                }

                kept.add(referenceKey);
            }

            references.put(structure, kept);
        });

        return references;
    }
}
