package com.toroidalworld.compat.create.client;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.compat.create.ThreadScope;
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
    private static final ThreadScope<Frame> PAYLOAD_FRAME = new ThreadScope<>();

    private record Frame(WorldLoopTransformer transformer, BlockPos anchor) {
    }

    public static <T> T readingPayload(Supplier<T> read) {
        return PAYLOAD_FRAME.with(resolve(), read);
    }

    public static BlockPos fold(BlockPos canonical) {
        Frame frame = PAYLOAD_FRAME.current();
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
