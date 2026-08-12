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

// F3 shows the client's own coordinate, which in a looped world is the unbounded one the packet layer feeds it — x grows
// past the seam while the player is really a few blocks the other side of it. This rewrites the three position lines to
// the true torus coordinate and inserts the unwrapped client frame as its own labeled triple right below them: when the
// two part ways, that is the seam being crossed, and a mismatch would show up here first.
//
// The three lines are found by their own prefix rather than by index: what precedes them varies (the server chunk stats
// line is conditional), and the reduced-debug-info branch returns a list without them at all — where finding nothing and
// passing through is exactly right, since that mode hides coordinates on purpose.
@Mixin(DebugScreenOverlay.class)
public class DebugEntryPositionMixin {
    @Unique
    private static final String XYZ_PREFIX = "XYZ: ";

    @Unique
    private static final String BLOCK_PREFIX = "Block: ";

    @Unique
    private static final String CHUNK_PREFIX = "Chunk: ";

    // Reads the client-only bounds store, not transformerOf: the level's own transformer is NOOP on the client by
    // design and must stay so. The bounds reach the client only through WrappingSettingsPayload; before it arrives, and
    // in an unwrapped world, the store is NOOP and this returns null, so the vanilla lines are left exactly as is.
    //
    // The list is rewritten in place and handed back: vanilla goes on adding to it after this returns, so it is a fresh
    // mutable list every call.
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
        // No region part on the unwrapped chunk line: region files on disk live in the wrapped frame, so a raw-frame
        // region name would point at a file that does not exist.
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
