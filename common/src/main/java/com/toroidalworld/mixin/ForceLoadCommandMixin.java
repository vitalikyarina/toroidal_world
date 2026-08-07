package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.ForceLoadCommand;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

// Reserving chunks harms nobody, so an ambiguous range is answered rather than refused: the pair is read the short way
// round. Three things then have to agree about which chunks that means.
//
// The range itself is folded before the command sees it, so the chunk count — and the 256-chunk limit measured on it —
// describe the reservation actually asked for instead of the world-wide one the raw corners spell out.
//
// A folded range runs past the bounds by construction, and the chunks it names are phantoms: reserving one would hold
// a chunk that never loads while the real ground on the other side stays unreserved. Both the reservation and every
// chunk the command reports back are therefore taken to the physical chunk.
@Mixin(ForceLoadCommand.class)
public class ForceLoadCommandMixin {
    @WrapMethod(method = "changeForceLoad")
    private static int toroidal$foldRequestedRange(CommandSourceStack source, ColumnPos from, ColumnPos to,
            boolean add, Operation<Integer> original) throws CommandSyntaxException {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(source.getLevel());
        if (transformer == null) {
            return original.call(source, from, to, add);
        }

        int minX = Math.min(from.x(), to.x());
        int maxX = Math.max(from.x(), to.x());
        int minZ = Math.min(from.z(), to.z());
        int maxZ = Math.max(from.z(), to.z());
        return original.call(
                source,
                new ColumnPos(
                        transformer.coords.x.foldSpanStart(minX, maxX),
                        transformer.coords.z.foldSpanStart(minZ, maxZ)),
                new ColumnPos(
                        transformer.coords.x.foldSpanEnd(minX, maxX),
                        transformer.coords.z.foldSpanEnd(minZ, maxZ)),
                add);
    }

    @WrapOperation(
            method = "changeForceLoad",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setChunkForced(IIZ)Z"))
    private static boolean toroidal$reserveThePhysicalChunk(ServerLevel level, int chunkX, int chunkZ, boolean forced,
            Operation<Boolean> original) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(level);
        if (transformer == null) {
            return original.call(level, chunkX, chunkZ, forced);
        }

        return original.call(level, transformer.chunks.x.wrap(chunkX), transformer.chunks.z.wrap(chunkZ), forced);
    }

    // Every chunk this command names to the player, which is three of them: the single chunk changed, and the two
    // corners of the range in the "added N chunks from … to …" message. No ordinal, on purpose — all three want the same
    // thing, and a coordinate the mod prints has to be one this world has. Refusing a player who types a chunk past the
    // bounds while printing one ourselves would be two rules for the same question.
    //
    // A range that crossed the seam therefore reads back with its corners the wrong way round — from [15, 0] to
    // [-16, 0]. That is what the reservation is: it starts at chunk 15, crosses the seam and ends at -16. A range
    // through the seam has no least and greatest corner to print, and of the two odd-looking ways to write it, this is
    // the one where both numbers name chunks that exist. The reversal is also the only hint in the message that the
    // seam was crossed at all.
    //
    // Should a Minecraft update add a fourth ChunkPos to this method, check that it wants wrapping too.
    @ModifyExpressionValue(
            method = "changeForceLoad",
            at = @At(value = "NEW", target = "net/minecraft/world/level/ChunkPos"))
    private static ChunkPos toroidal$reportThePhysicalChunk(ChunkPos chunkPos,
            @Local(argsOnly = true) CommandSourceStack source) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(source.getLevel());
        return transformer == null ? chunkPos : transformer.chunks.wrap(chunkPos);
    }
}
