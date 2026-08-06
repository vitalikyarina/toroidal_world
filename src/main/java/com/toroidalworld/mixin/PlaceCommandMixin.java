package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.toroidalworld.accessors.FramedStructureStart;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.PlaceCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

// /place is imperative and post-generation, so it never runs applyBiomeDecoration and the natural-gen structure fold
// does not touch it. Neither the loaded check nor the block writes need anything from here: checkLoaded asks
// level.isLoaded, which ServerChunkCacheMixin folds onto the physical chunk for every asker, and setBlock resolves its
// chunk through Level.getChunk, which LevelMixin already wraps — so a raw out-of-bounds write lands in the wrapped
// chunk at the correct local coordinate (world width is a multiple of 16).
//
// What is left is placeStructure, wrapped whole: its placement sits in a lambda a handler scoped to placeStructure
// cannot match, and each slice of the start has to be framed into the chunk that really holds it. Non-looped levels
// take the original untouched.
@Mixin(PlaceCommand.class)
public class PlaceCommandMixin {
    @Shadow
    @Final
    private static SimpleCommandExceptionType ERROR_STRUCTURE_FAILED;

    @Shadow
    private static void checkLoaded(ServerLevel level, ChunkPos chunkMin, ChunkPos chunkMax)
            throws CommandSyntaxException {
        throw new AssertionError();
    }

    @WrapMethod(method = "placeStructure")
    private static int toroidal$placeStructureAcrossSeam(
            CommandSourceStack source,
            Holder.Reference<Structure> structureHolder,
            BlockPos pos,
            Operation<Integer> original) throws CommandSyntaxException {
        ServerLevel level = source.getLevel();
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        if (transformer == null) {
            return original.call(source, structureHolder, pos);
        }

        Structure structure = structureHolder.value();
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        StructureStart start = structure.generate(
                structureHolder,
                level.dimension(),
                source.registryAccess(),
                generator,
                generator.getBiomeSource(),
                level.getChunkSource().randomState(),
                level.getStructureManager(),
                level.getSeed(),
                ChunkPos.containing(pos),
                0,
                level,
                b -> true);
        if (!start.isValid()) {
            throw ERROR_STRUCTURE_FAILED.create();
        }

        BoundingBox boundingBox = start.getBoundingBox();
        ChunkPos chunkMin = new ChunkPos(
                SectionPos.blockToSectionCoord(boundingBox.minX()), SectionPos.blockToSectionCoord(boundingBox.minZ()));
        ChunkPos chunkMax = new ChunkPos(
                SectionPos.blockToSectionCoord(boundingBox.maxX()), SectionPos.blockToSectionCoord(boundingBox.maxZ()));

        checkLoaded(level, chunkMin, chunkMax);

        FramedStructureStart framable = (FramedStructureStart) (Object) start;
        for (int chunkX = chunkMin.x(); chunkX <= chunkMax.x(); chunkX++) {
            for (int chunkZ = chunkMin.z(); chunkZ <= chunkMax.z(); chunkZ++) {
                ChunkPos wrapped = transformer.chunks.wrap(new ChunkPos(chunkX, chunkZ));

                // The slice belongs in the real chunk, so the start is moved into that chunk's frame first; an in-bounds
                // chunk has a zero shift and gets the start itself, byte-for-byte vanilla.
                StructureStart framed = framable.toroidal$framedBy(level, wrapped.x() - chunkX, wrapped.z() - chunkZ);
                if (framed == null) {
                    continue;
                }

                framed.placeInChunk(
                        level,
                        level.structureManager(),
                        generator,
                        level.getRandom(),
                        new BoundingBox(
                                wrapped.getMinBlockX(),
                                level.getMinY(),
                                wrapped.getMinBlockZ(),
                                wrapped.getMaxBlockX(),
                                level.getMaxY() + 1,
                                wrapped.getMaxBlockZ()),
                        wrapped);
            }
        }

        String id = structureHolder.key().identifier().toString();
        source.sendSuccess(
                () -> Component.translatable("commands.place.structure.success", id, pos.getX(), pos.getY(), pos.getZ()),
                true);
        return 1;
    }
}
