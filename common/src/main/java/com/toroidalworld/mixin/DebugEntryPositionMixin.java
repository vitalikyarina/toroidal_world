package com.toroidalworld.mixin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugEntryPosition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

// F3 shows the client's own coordinate, which in a looped world is the unbounded one the packet layer feeds it — x grows
// past the seam while the player is really a few blocks the other side of it. This rewrites the three position lines to
// the true torus coordinate and inserts the unwrapped client frame as its own labeled triple right below them: when the
// two part ways, that is the seam being crossed, and a mismatch would show up here first.
@Mixin(DebugEntryPosition.class)
public class DebugEntryPositionMixin {
    // Reads the client-only bounds store, not transformerOf: the level's own transformer is NOOP on the client by
    // design and must stay so. The bounds reach the client only through WrappingSettingsPayload; before it arrives, and
    // in an unwrapped world, the store is NOOP and this returns null, so the vanilla lines are left exactly as is.
    // The unwrapped triple goes inside the position group (index 3, right after Chunk:) rather than out as an own F3
    // group: DebugScreenOverlay deals groups to the two columns at (n+1)/2, so an extra group can open the other column
    // instead of standing below the position block.
    @ModifyArg(
            method = "display",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/debug/DebugScreenDisplayer;addToGroup(Lnet/minecraft/resources/Identifier;Ljava/util/Collection;)V"),
            index = 1)
    private Collection<String> toroidal$wrapPositionLines(Collection<String> lines) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null || lines.size() < 3) {
            return lines;
        }

        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedClientBoundsTransformerOf(level);
        Entity entity = minecraft.getCameraEntity();
        if (transformer == null || entity == null) {
            return lines;
        }

        double rawX = entity.getX();
        double rawZ = entity.getZ();
        BlockPos feet = entity.blockPosition();
        BlockPos wrappedFeet = transformer.blocks.wrap(feet);
        ChunkPos rawChunk = ChunkPos.containing(feet);
        ChunkPos wrappedChunk = transformer.chunks.wrap(rawChunk);

        List<String> wrapped = new ArrayList<>(lines);
        wrapped.set(0, String.format(Locale.ROOT, "XYZ: %.3f / %.5f / %.3f",
                transformer.coords.x.wrap(rawX), entity.getY(), transformer.coords.z.wrap(rawZ)));
        wrapped.set(1, String.format(Locale.ROOT, "Block: %d %d %d",
                wrappedFeet.getX(), wrappedFeet.getY(), wrappedFeet.getZ()));
        wrapped.set(2, String.format(Locale.ROOT, "Chunk: %d %d %d [%d %d in r.%d.%d.mca]",
                wrappedChunk.x(), SectionPos.blockToSectionCoord(feet.getY()), wrappedChunk.z(),
                wrappedChunk.getRegionLocalX(), wrappedChunk.getRegionLocalZ(),
                wrappedChunk.getRegionX(), wrappedChunk.getRegionZ()));
        // No region part on the unwrapped chunk line: region files on disk live in the wrapped frame, so a raw-frame
        // region name would point at a file that does not exist.
        wrapped.addAll(3, List.of(
                String.format(Locale.ROOT, "Unwrapped XYZ: %.3f / %.5f / %.3f", rawX, entity.getY(), rawZ),
                String.format(Locale.ROOT, "Unwrapped Block: %d %d %d", feet.getX(), feet.getY(), feet.getZ()),
                String.format(Locale.ROOT, "Unwrapped Chunk: %d %d %d",
                        rawChunk.x(), SectionPos.blockToSectionCoord(feet.getY()), rawChunk.z())));

        return wrapped;
    }
}
