package com.toroidalworld.engine.net;

import java.util.function.Supplier;

import com.mojang.logging.LogUtils;
import com.toroidalworld.compat.ModPresence;
import com.toroidalworld.engine.seam.ClientPosition;

import net.fabricmc.fabric.impl.menu.Networking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

public final class OpenMenuTranslation {
    private static final ModPresence GATE = ModPresence.of(LogUtils.getLogger(),
            "net/fabricmc/fabric/impl/menu/Networking$OpenScreenPayload.class",
            "[menu-api-compat] gate open_screen_present");

    public static void register() {
        if (GATE.present()) {
            Rewriter.register();
        }
    }

    private static final class Rewriter {
        static void register() {
            PacketTranslator.registerClientboundPayloadRewriter(
                    Networking.OpenScreenPayload.class, Rewriter::toClient);
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private static CustomPacketPayload toClient(Networking.OpenScreenPayload payload, TranslationContext context) {
            Object data = payload.data();
            if (data == null) {
                return payload;
            }

            Object clientData = FoldedValue.toward(context, mirrorAnchor(context), data);
            return clientData == data ? payload
                    : new Networking.OpenScreenPayload(payload.identifier(), payload.containerId(),
                            payload.title(), payload.innerCodec(), clientData);
        }

        private static Supplier<Vec3> mirrorAnchor(TranslationContext context) {
            return () -> {
                ClientPosition mirror = context.clientPosition();
                return new Vec3(mirror.x(), 0.0, mirror.z());
            };
        }

        private Rewriter() {
        }
    }

    private OpenMenuTranslation() {
    }
}
