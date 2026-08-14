package com.toroidalworld.compat.journeymap;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;

import com.toroidalworld.api.ToroidalShape;
import com.toroidalworld.api.ToroidalWorldClientApi;
import com.toroidalworld.ToroidalWorld;
import com.mojang.logging.LogUtils;

import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.client.display.DisplayType;
import journeymap.api.v2.client.display.PolygonOverlay;
import journeymap.api.v2.client.event.MappingEvent;
import journeymap.api.v2.common.JourneyMapPlugin;
import journeymap.api.v2.common.event.ClientEventRegistry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

// Registered with JourneyMap through its own plugin surface (annotation scan). The coordinate folds live in the
// mixins — the plugin API offers no hook over the cartography pipeline — so this carries the API-level part of the
// compat: the seam outline overlays, shown when JourneyMap starts mapping a wrapped dimension and cleared when it
// stops. compileOnly against the published API jar; the class only loads when JourneyMap itself instantiates it.
@JourneyMapPlugin(apiVersion = "2.0.0", dependencies = {}, require = false)
public class ToroidalJourneyMapPlugin implements IClientPlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private IClientAPI api;

    @Override
    public String getModId() {
        return ToroidalWorld.MODID;
    }

    @Override
    public void initialize(IClientAPI api) {
        this.api = api;
        ClientEventRegistry.MAPPING_EVENT.subscribe(ToroidalWorld.MODID, this::onMapping);
        LOGGER.info("[jm-compat] plugin initialized jm_api={}", api.getClass().getName());
    }

    private void onMapping(MappingEvent event) {
        if (event.getStage() == MappingEvent.Stage.MAPPING_STOPPED) {
            this.api.removeAll(ToroidalWorld.MODID);
            return;
        }

        // The bounds always precede mapping: the payload arrives during login/dimension change, before any chunk.
        // A level that does not match the event's dimension (or is absent) means the client is mid-switch — the
        // next MAPPING_STARTED for the real level will do the work.
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !level.dimension().equals(event.dimension)) {
            LOGGER.info("[jm-compat] seam_overlay dim={} shown=0 reason=level_mismatch", event.dimension.location());
            return;
        }

        Optional<ToroidalShape> shape = ToroidalWorldClientApi.shapeOf(level);
        if (shape.isEmpty() || !this.api.playerAccepts(ToroidalWorld.MODID, DisplayType.Polygon)) {
            LOGGER.info("[jm-compat] seam_overlay dim={} shown=0 wrapped={}",
                    event.dimension.location(), shape.isPresent());
            return;
        }

        List<PolygonOverlay> overlays = SeamOverlays.build(event.dimension, shape.get());
        int shown = 0;
        for (PolygonOverlay overlay : overlays) {
            try {
                this.api.show(overlay);
                shown++;
            } catch (Exception e) {
                LOGGER.warn("[jm-compat] seam_overlay show failed", e);
            }
        }

        LOGGER.info("[jm-compat] seam_overlay dim={} shown={}", event.dimension.location(), shown);
    }
}
