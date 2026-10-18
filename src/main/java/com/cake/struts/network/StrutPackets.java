package com.cake.struts.network;

import com.cake.struts.content.StrutBreakerHelper;
import com.cake.struts.content.StrutUseGuard;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class StrutPackets {

    private StrutPackets() {
    }

    // NeoForge: payloads are registered in RegisterPayloadHandlersEvent. Handlers run on the server thread by default.
    public static void register(final RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(BreakStrutPacket.TYPE, BreakStrutPacket.CODEC, (payload, context) -> {
            final ServerPlayer player = (ServerPlayer) context.player();
            StrutUseGuard.markBreak(player);
            StrutBreakerHelper.breakStrut(player, payload.target());
        });
    }
}
