package com.toroidalworld.storage;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.accessors.ClientBoundsHolder;
import com.toroidalworld.accessors.ClientPositionHolder;
import com.toroidalworld.accessors.TransformerCache;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.player.ClientPosition;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public final class WorldLoopAttachments {
    // A field read off the level: LevelMixin resolves the transformer once per level from its own chunk generator and
    // keeps it, because this is asked thousands of times per tick from the block-entity and scheduled-tick gates alone.
    public static WorldLoopTransformer transformerOf(Level level) {
        return ((TransformerCache) level).toroidal$transformer();
    }

    // The transformer only when the level actually wraps, else null — so a caller can fetch and bail in one step.
    public static @Nullable WorldLoopTransformer wrappedTransformerOf(Level level) {
        WorldLoopTransformer transformer = transformerOf(level);
        return transformer.isWrapped() ? transformer : null;
    }

    // The bounds the server told this client, held apart from the engine's transformer on purpose. That one drives the
    // whole wrapping engine, and on the client it MUST stay NOOP — the client is told the world is infinite, which is
    // what keeps rendering and chunk loading working across the seam. So the client's knowledge of the bounds lives in
    // ClientBoundsHolder on the client level, where only things that want to *read* the bounds without making the level
    // wrap will look (the debug overlay and the compass today). Server levels never implement it and fall out on the
    // instanceof; the default is NOOP, and the client sets it from WrappingSettingsPayload.
    private static WorldLoopTransformer clientBoundsTransformerOf(Level level) {
        return level instanceof ClientBoundsHolder holder ? holder.toroidal$clientBounds() : WorldLoopTransformer.NOOP;
    }

    public static @Nullable WorldLoopTransformer wrappedClientBoundsTransformerOf(Level level) {
        WorldLoopTransformer transformer = clientBoundsTransformerOf(level);
        return transformer.isWrapped() ? transformer : null;
    }

    // Held by the connection, not the player: death replaces the ServerPlayer, and PlayerList.respawn assigns the new
    // one to the listener only after it returns — so packets sent during the respawn would read a mirror belonging to
    // the body that just died. The client on the other end never changed, and neither does its connection.
    //
    // Only for a player who has one. Every caller here is driven by a packet, which cannot exist without a connection;
    // the one path that runs earlier is the seeding below, which checks.
    public static ClientPosition clientPositionOf(ServerPlayer player) {
        return ((ClientPositionHolder) player.connection).toroidal$clientPosition();
    }

    // The one formula for pointing the mirror at where the player now is. Three places need it — the connection being
    // opened, a respawn, and an arrival in another dimension — and they must agree: a mirror seeded a lap away from the
    // others sends the client's chunk cache a world from the chunks it is given. Those three are the whole list because
    // they are where the client builds a fresh player of its own and forgets the coordinate it was carrying; anywhere
    // else the client keeps counting, and so must the mirror.
    //
    // Onto the wrapped position, not the raw one. The client is starting a fresh space here, and starting it aligned
    // with the server's own truth is what keeps its coordinate meaningful. On a level that does not wrap the transformer
    // is NOOP and wrap() is the identity, so this is safe to call anywhere.
    public static void rebaseClientPositionOf(ServerPlayer player) {
        // The server places a player once while they are still being configured, before there is a connection to hold
        // the mirror. Nothing to seed then: the connection creates its own the moment it exists, from this same
        // position. The check belongs here rather than in each caller — this is the only path that can run that early.
        if (player.connection == null) {
            return;
        }

        WorldLoopTransformer transformer = transformerOf(player.level());
        clientPositionOf(player).rebase(
                transformer.coords.x.wrap(player.getX()),
                transformer.coords.z.wrap(player.getZ()),
                player.level().dimension(),
                transformer);
    }

    private WorldLoopAttachments() {
    }
}
