package com.toroidalworld.compat.journeymap;

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

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !level.dimension().equals(event.dimension)) {
            return;
        }

        Optional<ToroidalShape> shape = ToroidalWorldClientApi.shapeOf(level);
        if (shape.isEmpty() || !this.api.playerAccepts(ToroidalWorld.MODID, DisplayType.Polygon)) {
            return;
        }

        for (PolygonOverlay overlay : SeamOverlays.build(event.dimension, shape.get())) {
            try {
                this.api.show(overlay);
            } catch (Exception e) {
                LOGGER.warn("[jm-compat] seam_overlay show failed", e);
            }
        }
    }
}
