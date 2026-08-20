package com.toroidalworld.mixin;

import java.util.List;
import java.util.Locale;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

@Mixin(DebugScreenOverlay.class)
public class DebugEntryPositionMixin {
    @Unique
    private static final String XYZ_PREFIX = "XYZ: ";

    @Unique
    private static final String BLOCK_PREFIX = "Block: ";

    @Unique
    private static final String CHUNK_PREFIX = "Chunk: ";

    @ModifyReturnValue(method = "getGameInformation", at = @At("RETURN"))
    private List<String> toroidal$wrapPositionLines(List<String> lines) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        Entity entity = minecraft.getCameraEntity();
        if (level == null || entity == null) {
            return lines;
        }

        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedClientBoundsTransformerOf(level);
        if (transformer == null) {
            return lines;
        }

        int xyzLine = toroidal$lineStartingWith(lines, XYZ_PREFIX);
        int blockLine = toroidal$lineStartingWith(lines, BLOCK_PREFIX);
        int chunkLine = toroidal$lineStartingWith(lines, CHUNK_PREFIX);
        if (xyzLine < 0 || blockLine < 0 || chunkLine < 0) {
            return lines;
        }

        double rawX = entity.getX();
        double rawZ = entity.getZ();
        BlockPos feet = entity.blockPosition();
        BlockPos wrappedFeet = transformer.blocks.wrap(feet);
        ChunkPos rawChunk = new ChunkPos(feet);
        ChunkPos wrappedChunk = transformer.chunks.wrap(rawChunk);

        lines.set(xyzLine, String.format(Locale.ROOT, "XYZ: %.3f / %.5f / %.3f",
                transformer.coords.x.wrap(rawX), entity.getY(), transformer.coords.z.wrap(rawZ)));
        lines.set(blockLine, String.format(Locale.ROOT, "Block: %d %d %d [%d %d %d]",
                wrappedFeet.getX(), wrappedFeet.getY(), wrappedFeet.getZ(),
                wrappedFeet.getX() & 15, wrappedFeet.getY() & 15, wrappedFeet.getZ() & 15));
        lines.set(chunkLine, String.format(Locale.ROOT, "Chunk: %d %d %d [%d %d in r.%d.%d.mca]",
                wrappedChunk.x, SectionPos.blockToSectionCoord(feet.getY()), wrappedChunk.z,
                wrappedChunk.getRegionLocalX(), wrappedChunk.getRegionLocalZ(),
                wrappedChunk.getRegionX(), wrappedChunk.getRegionZ()));
        lines.addAll(chunkLine + 1, List.of(
                String.format(Locale.ROOT, "Unwrapped XYZ: %.3f / %.5f / %.3f", rawX, entity.getY(), rawZ),
                String.format(Locale.ROOT, "Unwrapped Block: %d %d %d", feet.getX(), feet.getY(), feet.getZ()),
                String.format(Locale.ROOT, "Unwrapped Chunk: %d %d %d",
                        rawChunk.x, SectionPos.blockToSectionCoord(feet.getY()), rawChunk.z)));

        return lines;
    }

    @Unique
    private static int toroidal$lineStartingWith(List<String> lines, String prefix) {
        for (int index = 0; index < lines.size(); index++) {
            if (lines.get(index).startsWith(prefix)) {
                return index;
            }
        }

        return -1;
    }
}
