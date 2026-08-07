package com.toroidalworld;

import com.toroidalworld.config.WorldLoopConfig;
import com.toroidalworld.gen.WorldLoopGenerators;
import com.toroidalworld.gen.WorldLoopTicketTypes;
import com.toroidalworld.net.AuxiliaryLightTranslation;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// The one wiring point for the looped world feature. The mod entrypoint calls init and knows nothing else about the
// feature's internals — the pattern a second feature in this mod would follow. Self-registering pieces
// (@EventBusSubscriber: shape setup, network, sync) are not listed here; they wire themselves.
public final class WorldLoop {
    public static void init(IEventBus modEventBus, ModContainer modContainer) {
        WorldLoopGenerators.CHUNK_GENERATORS.register(modEventBus);
        WorldLoopTicketTypes.TICKET_TYPES.register(modEventBus);
        AuxiliaryLightTranslation.register();
        modContainer.registerConfig(ModConfig.Type.CLIENT, WorldLoopConfig.SPEC);

        // The spec alone only yields the toml; the mod-list Config button needs a screen factory. NeoForge's generic
        // ConfigurationScreen builds the UI from the registered specs. Guarded because init also runs on the dedicated
        // server, where the screen classes do not exist; the method reference is only materialised inside.
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        }
    }

    private WorldLoop() {
    }
}
