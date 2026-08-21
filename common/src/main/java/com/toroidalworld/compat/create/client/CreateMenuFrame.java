package com.toroidalworld.compat.create.client;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

// The menu payload is opaque to the packet layer, so its positions arrive canonical while the client names the same
// blocks in its own frame; they are folded as they are read rather than where they are looked up, because
// FactoryPanelBehaviour.at refuses a canonical position at its isLoaded gate before any lookup happens.
public final class CreateMenuFrame {
    private static final ThreadLocal<@Nullable Frame> PAYLOAD_FRAME = new ThreadLocal<>();

    private record Frame(WorldLoopTransformer transformer, BlockPos anchor) {
    }

    public static <T> T readingPayload(Supplier<T> read) {
        Frame previous = PAYLOAD_FRAME.get();
        PAYLOAD_FRAME.set(resolve());

        try {
            return read.get();
        } finally {
            PAYLOAD_FRAME.set(previous);
        }
    }

    public static BlockPos fold(BlockPos canonical) {
        Frame frame = PAYLOAD_FRAME.get();
        if (frame == null) {
            return canonical;
        }

        return frame.transformer().blocks.nearestCopy(frame.anchor(), canonical);
    }

    private static @Nullable Frame resolve() {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            return null;
        }

        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedClientBoundsTransformerOf(level);
        return transformer == null ? null : new Frame(transformer, player.blockPosition());
    }

    private CreateMenuFrame() {
    }
}
