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

@Mixin(DebugEntryPosition.class)
public class DebugEntryPositionMixin {
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
        wrapped.addAll(3, List.of(
                String.format(Locale.ROOT, "Unwrapped XYZ: %.3f / %.5f / %.3f", rawX, entity.getY(), rawZ),
                String.format(Locale.ROOT, "Unwrapped Block: %d %d %d", feet.getX(), feet.getY(), feet.getZ()),
                String.format(Locale.ROOT, "Unwrapped Chunk: %d %d %d",
                        rawChunk.x(), SectionPos.blockToSectionCoord(feet.getY()), rawChunk.z())));

        return wrapped;
    }
}
