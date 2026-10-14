package com.vv.sporeadjust.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.function.Consumer;

public final class SANetwork {
    // Installed during client setup. Common registration never loads client-only classes.
    private static Consumer<ReinforcementSmokePayload> smokeReceiver = payload -> {};

    private SANetwork() {}

    public static void setSmokeReceiver(Consumer<ReinforcementSmokePayload> receiver) {
        smokeReceiver = receiver;
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(ReinforcementSmokePayload.TYPE,
                ReinforcementSmokePayload.STREAM_CODEC, (payload, context) -> smokeReceiver.accept(payload));
    }
}
