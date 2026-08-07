package com.toroidalworld;

import com.toroidalworld.config.WorldLoopConfig;
import com.toroidalworld.gen.LoopedChunkGenerator;
import com.toroidalworld.gen.LoopedFlatChunkGenerator;
import com.toroidalworld.gen.WorldLoopGenerators;
import com.toroidalworld.gen.WorldLoopTicketTypes;
import com.toroidalworld.net.AuxiliaryLightTranslation;
import com.toroidalworld.net.BlockParticleTranslation;
import com.toroidalworld.platform.NeoForgePlatform;
import com.toroidalworld.platform.Platforms;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.registries.DeferredRegister;

// The one wiring point for the looped world feature, and the NeoForge half of its loader seam: the platform
// implementation, the game-registry entries and the payload rewriters are all bound here, so the classes they wire
// stay loader-free. The mod entrypoint calls init and knows nothing else about the feature's internals — the pattern
// a second feature in this mod would follow. Self-registering pieces (@EventBusSubscriber: shape setup, network) are
// not listed here; they wire themselves.
public final class WorldLoop {
    private static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, ToroidalWorld.MODID);

    private static final DeferredRegister<TicketType> TICKET_TYPES =
            DeferredRegister.create(Registries.TICKET_TYPE, ToroidalWorld.MODID);

    public static void init(IEventBus modEventBus, ModContainer modContainer) {
        Platforms.set(new NeoForgePlatform());

        CHUNK_GENERATORS.register(WorldLoopGenerators.TOROIDAL_ID, () -> LoopedChunkGenerator.CODEC);
        CHUNK_GENERATORS.register(WorldLoopGenerators.TOROIDAL_FLAT_ID, () -> LoopedFlatChunkGenerator.CODEC);
        CHUNK_GENERATORS.register(modEventBus);

        TICKET_TYPES.register(WorldLoopTicketTypes.SEAM_GENERATION_ID, () -> WorldLoopTicketTypes.SEAM_GENERATION);
        TICKET_TYPES.register(modEventBus);

        AuxiliaryLightTranslation.register();
        BlockParticleTranslation.register();
        modContainer.registerConfig(ModConfig.Type.CLIENT, WorldLoopConfig.SPEC);

        // The spec alone only yields the toml; the mod-list Config button needs a screen factory. NeoForge's generic
        // ConfigurationScreen builds the UI from the registered specs. Guarded because init also runs on the dedicated
        // server, where the screen classes do not exist; the method reference is only materialised inside.
        if (Platforms.get().isClient()) {
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        }
    }

    private WorldLoop() {
    }
}
