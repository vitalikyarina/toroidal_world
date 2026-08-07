package com.toroidalworld.net;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.platform.Platforms;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.server.level.ServerPlayer;

// Only when the level actually wraps: an unwrapped one leaves the client's transformer NOOP, which is the vanilla
// path, and a vanilla client (the payload is optional) simply never receives it. The two wrapped dimensions carry
// different widths, so this is re-sent on every space change (login, dimension change — the two mixin call sites),
// not only login. The wrapped-or-not decision is the mod's own; only the send itself is the loader's.
public final class WrappingBoundsSync {
    public static void sendTo(ServerPlayer player) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(player.level());
        if (transformer != null) {
            Platforms.get().sendWrappingBounds(player, transformer.bounds);
        }
    }

    private WrappingBoundsSync() {
    }
}
