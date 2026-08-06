package com.toroidalworld.storage;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.accessors.ClientPositionHolder;
import com.toroidalworld.accessors.TransformerCache;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.gen.ShapedChunkGenerator;
import com.toroidalworld.player.ClientPosition;
import com.toroidalworld.ToroidalWorld;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class WorldLoopAttachments {
    private static final String DIMENSION_TRANSFORMER_ID = "dimension_transformer";
    private static final String CLIENT_BOUNDS_TRANSFORMER_ID = "client_bounds_transformer";

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ToroidalWorld.MODID);

    public static final Supplier<AttachmentType<WorldLoopTransformer>> DIMENSION_TRANSFORMER = ATTACHMENT_TYPES.register(
            DIMENSION_TRANSFORMER_ID,
            () -> AttachmentType.<WorldLoopTransformer>builder(WorldLoopAttachments::createTransformer).build());

    // A field read off the level, not an attachment lookup: LevelMixin resolves the attachment once per level and
    // keeps it, because this is asked thousands of times per tick from the block-entity and scheduled-tick gates alone.
    public static WorldLoopTransformer transformerOf(Level level) {
        return ((TransformerCache) level).toroidal$transformer();
    }

    // The transformer only when the level actually wraps, else null — so a caller can fetch and bail in one step.
    public static @Nullable WorldLoopTransformer wrappedTransformerOf(Level level) {
        WorldLoopTransformer transformer = transformerOf(level);
        return transformer.isWrapped() ? transformer : null;
    }

    // The bounds the server told this client, held apart from DIMENSION_TRANSFORMER on purpose. That one drives the whole
    // wrapping engine, and on the client it MUST stay NOOP — the client is told the world is infinite, which is what
    // keeps rendering and chunk loading working across the seam. So the client's knowledge of the bounds lives here,
    // where only things that want to *read* the bounds without making the level wrap will look (the debug overlay today).
    // Server levels never touch it; the default is NOOP, and the client sets it from WrappingSettingsPayload.
    public static final Supplier<AttachmentType<WorldLoopTransformer>> CLIENT_BOUNDS_TRANSFORMER = ATTACHMENT_TYPES.register(
            CLIENT_BOUNDS_TRANSFORMER_ID,
            () -> AttachmentType.<WorldLoopTransformer>builder(holder -> WorldLoopTransformer.NOOP).build());

    private static WorldLoopTransformer clientBoundsTransformerOf(Level level) {
        return level.getData(CLIENT_BOUNDS_TRANSFORMER);
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
    // opened, the server placing a player, and an arrival in another dimension — and they must agree: a mirror seeded a
    // lap away from the others sends the client's chunk cache a world from the chunks it is given.
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

    // The bounds come from the level's own chunk generator. A looped world is created with the Looped world shape,
    // which rebuilds the overworld generator as a LoopedChunkGenerator carrying them — and vanilla persists a world's
    // generators. The shape itself is never stored, so the generator is the one thing that can still answer "does this
    // level wrap, and how wide" after a restart.
    private static WorldLoopTransformer createTransformer(IAttachmentHolder holder) {
        if (!(holder instanceof ServerLevel serverLevel)) {
            return WorldLoopTransformer.NOOP;
        }

        if (serverLevel.getChunkSource().getGenerator() instanceof ShapedChunkGenerator shaped) {
            return shaped.transformer();
        }

        return WorldLoopTransformer.NOOP;
    }

    private WorldLoopAttachments() {
    }
}
